package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.RecoveryOuterClass;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import server.RecoveryManager;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMessageListener implements AdvancedMessageListener {

    private final SocketInfo serverInfo;
    
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> request_queue = new ConcurrentQueue<>();
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> replication_queue = new ConcurrentQueue<>();
    private final AtomicLong request_counter = new AtomicLong();
    private final AtomicLong replication_counter = new AtomicLong();

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    private Map<String,Integer> need_recovery = new HashMap<>();
    private boolean recovery = true;

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

    private void handleAssignmentMessage(SpreadMessage spreadMessage, Message message) throws InvalidProtocolBufferException {

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

        if (!spreadMessage.getSender().toString().equals(this.myself)){

            switch (message.getRecovery().getTypeCase()) {

                case NUMBEROFLINES:

                    List<Pair<Integer,String>> lines = RecoveryManager.getRecoveryLines(this.serverInfo.getPort(), message.getRecovery().getNumberOfLines().getLines(),
                            this.need_recovery.get(spreadMessage.getSender().toString()));

                    List<RecoveryOuterClass.Lines.Line> lines_proto = new ArrayList<>();

                    for (Pair<Integer,String> line : lines)
                        lines_proto.add(
                                RecoveryOuterClass.Lines.Line.newBuilder()
                                        .setNumber(line.getFirst())
                                        .setData(line.getSecond())
                                        .build());

                    Message msg = Message.newBuilder()
                            .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                                    .setLines(RecoveryOuterClass.Lines.newBuilder()
                                            .addAllLines(lines_proto)
                                            .setMin(message.getRecovery().getNumberOfLines().getLines())
                                            .setMax(this.need_recovery.get(spreadMessage.getSender().toString()))
                                            .build())
                                    .build())
                            .build();

                    SpreadConnector.cast(msg.toByteArray(), Set.of(spreadMessage.getSender().toString()));

                    this.need_recovery.remove(spreadMessage.getSender().toString());

                    break;

                case LINES:
                    if(this.recovery) {

                        RecoveryManager.recover(this.serverInfo.getPort(), message.getRecovery().getLines());
                        this.recovery = false;

                    }

                    break;

            }

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
        if (this.leader_fifo.get(0).equals(this.myself) && !this.primary)
            this.primary = true;


        // check if i need recovery or to recover someone
        if (info.isCausedByJoin()) {

            if (!this.primary && this.first_message && this.recovery) {

                System.out.println("\nNeed Recover\n");
                this.first_message = false;

                // get the size of the DB
                Message msg = constructRecoveryNumberOfLines(RecoveryManager.getCurrentSize(this.serverInfo.getPort()));
                // send to group my info
                SpreadConnector.cast(msg.toByteArray(), Set.of("Servers"));

            }

            // it was me that joined, and i'm primary, nobody needs recovery
            else if (this.primary && this.first_message)
                this.first_message = false;

            // there is somebody that needs recovery
            else {
                System.out.println("\nSomeone need recover\n");

                this.need_recovery.put(info.getJoined().toString(), RecoveryManager.getCurrentSize(this.serverInfo.getPort()));
            }
        }

    }

    public String getMyself(){ return this.myself;}

    public boolean isPrimary() {return this.primary;}

    public Triplet<Boolean, Long, Message> getNextRequest() { return this.request_queue.poll(); }

    public Triplet<Boolean,Long,Message> getNextReplication() { return this.replication_queue.poll(); }

    private Message constructRecoveryNumberOfLines(int lines) {
        return Message.newBuilder()
                .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                        .setNumberOfLines(RecoveryOuterClass.NumberOfLines.newBuilder()
                                .setLines(lines)
                                .build())
                        .build())
                .build();
    }
}
