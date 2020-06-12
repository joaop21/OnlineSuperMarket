package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import database.DatabaseManager;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.RecoveryOuterClass;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import server.RequestManager;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    /* Info for primary selection */
    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    /* Info for recovery */
    private boolean recovery = true;
    private final Lock recovery_lock = new ReentrantLock();
    private final Condition recovered = this.recovery_lock.newCondition();
    private Map<String,Message> nedd_recover = new HashMap<>();

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

                this.nedd_recover.remove(spreadMessage.getSender().toString());

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

                } else {

                    // store info
                    Message msg = RecoveryManager.getRecoverInfo(this.serverInfo.getPort(), info.getJoined());
                    this.nedd_recover.put(info.getJoined().toString(), msg);

                }

            }

        }

    }

    public String getMyself(){ return this.myself;}

    public boolean isPrimary() {return this.primary;}

    public Triplet<Boolean, Long, Message> getNextRequest() { return this.request_queue.poll(); }

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
        for(Map.Entry<String,Message> info : this.nedd_recover.entrySet()) {

            SpreadConnector.unicast(info.getValue().toByteArray(), info.getKey());

            this.nedd_recover.remove(info.getKey());

        }


    }

}
