package middleware.loadbalancer;

import com.google.protobuf.InvalidProtocolBufferException;
import loadbalancer.LoadBalancerSkeleton;
import middleware.gateway.Gateway;
import middleware.proto.MessageOuterClass.Message;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LoadBalancerMessageListener implements AdvancedMessageListener  {

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    private int port;
    private Map<Integer, Integer> server_ports;

    public LoadBalancerMessageListener (int port) { this.port = port; }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        try {

            Message message = Message.parseFrom(spreadMessage.getData());

            //System.out.println();

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

        }

    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {

        MembershipInfo info = spreadMessage.getMembershipInfo();

        switch (info.getGroup().toString()) {

            case "LoadBalancing":
                handleLoadBalancingInfo(info);
                break;

            case "System":
                handleSystemInfo(info);
                break;

        }
    }

    private void handleLoadBalancingInfo (MembershipInfo info) {

        if (info.isCausedByJoin()) { // Someone joined the arena

            if (this.first_message) {

                this.first_message = false;
                this.myself = info.getJoined().toString();

                for (SpreadGroup g : info.getMembers())
                    if (!g.toString().equals(myself))
                        this.leader_fifo.add(g.toString());

            }

            leader_fifo.add(info.getJoined().toString());

        } else if (info.isCausedByDisconnect()) // Someone disconnected from the arena

            this.leader_fifo.removeIf(member -> member.equals(info.getDisconnected().toString()));

        else if (info.isCausedByLeave()) // Someone left the arena

            this.leader_fifo.removeIf(member -> member.equals(info.getLeft().toString()));

        // Becoming master of the arena
        if (this.leader_fifo.get(0).equals(this.myself) && !this.primary) {

            // Setting myself as primary
            this.primary = true;

            // Starting acceptor of client connections
            //(new LoadBalancerSocketAcceptor()).run();
            new Gateway(this.port, LoadBalancerSkeleton.class);

        }

        if (this.primary) System.out.println("I'm primary!");
        else System.out.println("I'm useless!");

    }

    private void handleSystemInfo (MembershipInfo info) {}

    public boolean getPrimary(){ return this.primary;}
}
