package middleware.loadBalancer;

import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class LoadBalancerMessageListener implements AdvancedMessageListener  {

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;
    private boolean primary = false;

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        if (this.primary) System.out.println("I'm primary so I can do stuff");
        else System.out.println("I'm useless");

    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo info = spreadMessage.getMembershipInfo();

        if (info.isCausedByJoin()) {
            if (this.first_message) {
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

            // Setting myself as primary
            this.primary = true;

        }

        if (this.primary) System.out.println("I'm primary so I can do stuff");
        else System.out.println("I'm useless");
    }

    public boolean getPrimary(){ return this.primary;}
}
