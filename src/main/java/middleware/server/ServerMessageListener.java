package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ServerMessageListener implements AdvancedMessageListener {

    private SocketInfo serverInfo;
    
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> request_queue = new ConcurrentQueue<>();
    private final ConcurrentQueue<Triplet<Boolean, Long, Message>> replication_queue = new ConcurrentQueue<>();
    private final AtomicLong request_counter = new AtomicLong();
    private final AtomicLong replication_counter = new AtomicLong();

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;
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

        // check if i need recovery
        if (!this.primary && this.first_message && this.recovery) {

            this.first_message = false;
            // initialize thread for recovery

        }




    }

    public String getMyself(){ return this.myself;}

    public boolean isPrimary() {return this.primary;}

    public Triplet<Boolean, Long, Message> getNextRequest() { return this.request_queue.poll(); }

    public Triplet<Boolean,Long,Message> getNextReplication() { return this.replication_queue.poll(); }
}
