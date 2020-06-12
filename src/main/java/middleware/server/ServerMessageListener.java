package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MapEntry;
import database.DatabaseManager;
import database.DatabaseModification;
import database.QueryCart;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.RecoveryOuterClass;
import middleware.proto.ReplicationOuterClass;
import middleware.proto.RequestOuterClass;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;

import org.w3c.dom.CDATASection;

import server.RequestManager;

import server.Server;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class ServerMessageListener implements AdvancedMessageListener {

    /* Info about the socket */
    private final SocketInfo serverInfo;

    /* Requests info */
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> request_queue = new ConcurrentQueue<>();
    private final AtomicLong request_counter = new AtomicLong();

    /* Replication info */
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> replication_queue = new ConcurrentQueue<>();
    private final AtomicLong replication_counter = new AtomicLong();
    private final Lock replication_lock = new ReentrantLock();
    private final Condition empty_replication_queue = this.replication_lock.newCondition();

    private final ConcurrentHashMap<Integer, Pair<LocalDateTime, Long>> tmax_timestamps = new ConcurrentHashMap<>();

    /* Info for primary selection */
    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    /* Info for recovery */
    private boolean recovery = true;
    private final Lock recovery_lock = new ReentrantLock();
    private final Condition recovered = this.recovery_lock.newCondition();
    private Map<String,Message> need_recover = new HashMap<>();

    public ServerMessageListener (SocketInfo serverInfo) { this.serverInfo = serverInfo; }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        try {

            Message message = Message.parseFrom(spreadMessage.getData());

            switch (message.getTypeCase()){

                case ASSIGNMENT:
                    handleAssignmentMessage(spreadMessage, message);
                    break;

                case REQUEST:
                    handleRequestMessage(spreadMessage, message);
                    break;

                case REPLICATION:
                    handleReplicationMessage(spreadMessage, message);
                    break;

                case RECOVERY:
                    handleRecoveryMessage(spreadMessage, message);
                    break;

            }

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

        }

    }

    private void handleAssignmentMessage(SpreadMessage spreadMessage, Message message) {

        System.out.println("Received Assignment Message!");

        if (message.getAssignment().hasLoadBalancerInfo()) {

            System.out.println("Received Load Balancer Info!");

            message = Message.newBuilder()
                    .setAssignment(Assignment.newBuilder()
                            .setServerInfo(
                                    ServerInfo.newBuilder()
                                            .setAddress(serverInfo.getAddress())
                                            .setPort(serverInfo.getPort())
                                            .build()
                            )
                            .build())
                    .build();

            System.out.println("Sending Server Info back!");

            SpreadConnector.send(message.toByteArray(), spreadMessage.getSender());

        }

    }

    private void handleRequestMessage(SpreadMessage spreadMessage, Message message){

        boolean from_myself = false;
        if (spreadMessage.getSender().toString().equals(this.myself)) from_myself = true;

        this.request_queue.add(new Triplet<>(from_myself, request_counter.incrementAndGet(), message));

    }

    private void handleReplicationMessage(SpreadMessage spreadMessage, Message message){

        boolean from_myself = false;
        if (spreadMessage.getSender().toString().equals(this.myself)) from_myself = true;

        this.replication_queue.add(new Triplet<>(from_myself, replication_counter.incrementAndGet(), message));

    }

    private void handleRecoveryMessage(SpreadMessage spreadMessage, Message message) {

        switch (message.getRecovery().getType()) {

            case RECOVER:

                if (this.recovery){

                    // Make me recover
                    RecoveryManager.recoverMe(this.serverInfo.getPort(), message);

                    // Open database
                    DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/" + this.serverInfo.getPort() + "/onlinesupermarket");

                    Message msg = Message.newBuilder()
                            .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                                    .setType(RecoveryOuterClass.Recovery.Type.ACK)
                                    .build())
                            .build();

                    SpreadConnector.cast(msg.toByteArray(), Set.of("Servers"));

                    this.recovery_lock.lock();
                    this.recovery = false;
                    this.recovered.signal();
                    this.recovery_lock.unlock();

                }

                break;

            case ACK:

                this.need_recover.remove(spreadMessage.getSender().toString());

                break;

        }

    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {

        MembershipInfo info = spreadMessage.getMembershipInfo();

        switch (info.getGroup().toString()) {

            case "Servers":
                handleServerInfo(info);
                break;

            case "System":
                handleSystemInfo(info);
                break;

        }

    }

    public void handleSystemInfo (MembershipInfo info) {}

    public void handleServerInfo (MembershipInfo info) {

        updateHierarchy(info);

        handleRecover(info);

    }

    private void updateHierarchy(MembershipInfo info) {

        if (info.isCausedByJoin()) {

            if (this.first_message) {  // I joined

                this.myself = info.getJoined().toString();

                if (info.getMembers().length > 1){
                    for(SpreadGroup g : info.getMembers())
                        if(!g.toString().equals(myself))
                            this.leader_fifo.add(g.toString());
                }

            }

            leader_fifo.add(info.getJoined().toString());
        }
        else if (info.isCausedByDisconnect())
            this.leader_fifo.removeIf(member -> member.equals(info.getDisconnected().toString()));

        else if (info.isCausedByLeave())
            this.leader_fifo.removeIf(member -> member.equals(info.getLeft().toString()));

        // check if i am the primary server
        if (this.leader_fifo.get(0).equals(this.myself) && !this.primary) {

            this.primary = true;

            recoverTheUnrecovered();

            startTimers();

        }

    }

    // Starts Timers
    private void startTimers() {

        System.out.println("Starting the timers!");
        // Start timers
        for (Map.Entry<Integer, Pair<LocalDateTime, Long>> timestamp: tmax_timestamps.entrySet()) {

            int userId = timestamp.getKey();
            long delay = timestamp.getValue().getSecond() - ( timestamp.getValue().getFirst().until(LocalDateTime.now(), ChronoUnit.SECONDS) );

            Message msg = Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setType(RequestOuterClass.Request.Type.REQUEST)
                            .setCleanCart(RequestOuterClass.CleanCart.newBuilder()
                                    .setUserId(userId)
                                    .build())
                            .build())
                    .build();

            System.out.println("Timer of " + userId + " and delay " + delay);

            TimerTask task = new TimerTask() {
                public void run() {

                    request_queue.add(new Triplet<>(true, request_counter.incrementAndGet(), msg));

                }
            };
            new Timer("Timer").schedule(task, (delay >= 0) ? delay * 1000 : 0);

        }

    }

    private void handleRecover(MembershipInfo info) {

        // check if i need recovery or to recover someone
        if (info.isCausedByJoin()) {

            // it was me that joined, and i need recover
            if (!this.primary && this.first_message && this.recovery) {

                this.first_message = false;

                // check if a DB already exists
                if(!RecoveryManager.directoryExists("databases/" + this.serverInfo.getPort() + "/")) {

                    // create a database
                    DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/" + this.serverInfo.getPort() + "/onlinesupermarket");

                    // make a backup
                    RecoveryManager.backup("initial");

                } else {

                    // Open DB makes an automatic checkpoint and cleans the log file
                    DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/" + this.serverInfo.getPort() + "/onlinesupermarket");

                }

            }

            // it was me that joined, and i'm primary, nobody needs recover
            else if (this.primary && this.first_message) {

                this.first_message = false;

                // Create or open a database
                DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/" + this.serverInfo.getPort() + "/onlinesupermarket");

                // Make a backup
                RecoveryManager.backup("initial");

                // Recovered
                this.recovery_lock.lock();
                this.recovery = false;
                this.recovered.signal();
                this.recovery_lock.unlock();
            }

            // there is somebody that needs recover
            else {

                // Wait for the replication and request queue to be empty
                waitToEmptyReplication();
                waitToEmptyRequest();

                // only the primary recovers the new server, secondaries store the message just in case its needed
                if(this.primary) {

                    // Recover the new server
                    RecoveryManager.recoverSomeone(this.serverInfo.getPort(), info.getJoined());

                    recoverTimers(info);

                } else {

                    // store info
                    Message msg = RecoveryManager.getRecoverInfo(this.serverInfo.getPort(), info.getJoined());
                    this.need_recover.put(info.getJoined().toString(), msg);

                }

            }

        }

    }

    private void recoverTimers(MembershipInfo info) {

        System.out.println("Sending timers to new server!");
        // Recover timed actions
        List<ReplicationOuterClass.PeriodicActions.CleanCartInfo> cleanCartInfoList =
                tmax_timestamps.entrySet().stream().map(timestamp -> {
                    int userId = timestamp.getKey();
                    long delay = timestamp.getValue().getSecond() - ( timestamp.getValue().getFirst().until(LocalDateTime.now(), ChronoUnit.SECONDS) );

                    System.out.println("Timer of " + userId + " and delay " + delay);

                    return ReplicationOuterClass.PeriodicActions.CleanCartInfo.newBuilder()
                            .setUserId(userId)
                            .setDelay(delay)
                            .build();
                }).collect(Collectors.toList());

        Message msg = Message.newBuilder()
                .setReplication(ReplicationOuterClass.Replication.newBuilder()
                        .setPeriodics(ReplicationOuterClass.PeriodicActions.newBuilder()
                                .addAllCleanCartInfo(cleanCartInfoList)
                                .build())
                        .build())
                .build();

        SpreadConnector.send(msg.toByteArray(), info.getJoined());

    }

    public String getMyself(){ return this.myself;}

    public boolean isPrimary() {return this.primary;}

    public Triplet<Boolean, Long, Message> getNextRequest() { return this.request_queue.poll(); }

    // TMAX Management

    public void addTimestampTMAX (int userID, Pair<LocalDateTime, Long> timerInfo) {

        System.out.println("Adding timestamp of " + userID + " with delay " + timerInfo.getSecond());

        this.tmax_timestamps.put(userID, timerInfo);

    }

    public void remTimestampTMAX (int userID) {

        System.out.println("Removing timestamp of " + userID);

        this.tmax_timestamps.remove(userID);

    }

    public Pair<LocalDateTime, Long> getTimestampTMAX (int userID) {return this.tmax_timestamps.get(userID); }


    public Triplet<Boolean,Long,Message> getNextReplication() {

        // REPORT IF THE MODIFICATIONS WERE ALREADY CONSUMED
        this.replication_lock.lock();

        if (this.replication_queue.size() == 0)
            this.empty_replication_queue.signal();

        this.replication_lock.unlock();


        return this.replication_queue.poll();

    }

    private void waitToEmptyRequest() {

        RequestManager.waitToEmpty();

    }

    private void waitToEmptyReplication() {

        try {

            this.replication_lock.lock();

            while(this.replication_queue.size() != 0) {
                this.empty_replication_queue.await();
            }

            this.replication_lock.unlock();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void waitToRecover() {

        try {

            this.recovery_lock.lock();

            while(this.recovery)
                this.recovered.await();

        } catch (InterruptedException e) {

            e.printStackTrace();

        } finally {

            this.recovery_lock.unlock();

        }

    }

    private void recoverTheUnrecovered() {

        // send recover messages to the non-recovered
        for(Map.Entry<String,Message> info : this.need_recover.entrySet()) {

            SpreadConnector.unicast(info.getValue().toByteArray(), info.getKey());

            this.need_recover.remove(info.getKey());

        }


    }

}
