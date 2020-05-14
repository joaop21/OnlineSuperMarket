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

public class ServerMessageListener implements AdvancedMessageListener {

    private SocketInfo serverInfo;
    
    private final ConcurrentQueue<Triplet<Boolean, Integer, Message>> queue = new ConcurrentQueue<>();
    private int message_counter = 0;

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    public ServerMessageListener (SocketInfo serverInfo) { this.serverInfo = serverInfo; }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        try {

            Message message = Message.parseFrom(spreadMessage.getData());

            if (message.hasAssignment()) handleAssignmentMessage (spreadMessage);
            else {

                boolean from_myself = false;
                if (spreadMessage.getSender().toString().equals(this.myself)) from_myself = true;
                    
                this.queue.add(new Triplet<>(from_myself, message_counter++, Message.parseFrom(spreadMessage.getData())));

            }

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

        }

    }

    private void handleAssignmentMessage(SpreadMessage spreadMessage) throws InvalidProtocolBufferException {

        Message message = Message.parseFrom(spreadMessage.getData());

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

    public Triplet<Boolean, Integer, Message> getMessage() { return this.queue.poll(); }

    public void handleSystemInfo (MembershipInfo info) {}

    public void handleServerInfo (MembershipInfo info) {

        if (info.isCausedByJoin()) {

            if (this.first_message) {  // I joined

                this.first_message = false;
                this.myself = info.getJoined().toString();

                if (info.getMembers().length > 1){
                    for(SpreadGroup g : info.getMembers())
                        if(!g.toString().equals(myself))
                            this.leader_fifo.add(g.toString());
                }

            }

            leader_fifo.add(info.getJoined().toString());
        }
        else if(info.isCausedByDisconnect())
            this.leader_fifo.removeIf(member -> member.equals(info.getDisconnected().toString()));

        else if (info.isCausedByLeave())
            this.leader_fifo.removeIf(member -> member.equals(info.getLeft().toString()));

        if (this.leader_fifo.get(0).equals(this.myself) && !this.primary) {
            this.primary = true;
            System.out.println("I'm the Boss");
        }

    }

    public boolean isPrimary() {return this.primary;}
}
