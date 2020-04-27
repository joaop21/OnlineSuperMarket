package Middleware.LoadBalancer;

import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.LinkedList;
import java.util.List;

public class LBMessageListener implements AdvancedMessageListener  {
    private List<String> leader_fifo = new LinkedList<>();
    private String myself;
    private boolean first_message = true;

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        // do nothing for now
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

        for(int i = 0 ; i < this.leader_fifo.size() ; i++) {
            if (this.leader_fifo.get(i).equals(this.myself) && i == 0) {
                System.out.println("I'm Primary Server");
                break;
            }
            else if (!this.leader_fifo.get(i).equals(this.myself) && i==0)
                System.out.print("Before me: "+this.leader_fifo.get(i));
            else if (!this.leader_fifo.get(i).equals(this.myself) && i!=0)
                System.out.print(" , "+this.leader_fifo.get(i));
            else if (this.leader_fifo.get(i).equals(this.myself) && i!=0) {
                System.out.println("");
                break;
            }
        }
    }
}
