package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import middleware.proto.MessageOuterClass;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadMessage;

public class ServerMessageListener implements AdvancedMessageListener {
    private final ConcurrentQueue<Triplet<Boolean, Integer, MessageOuterClass.Message>> queue = new ConcurrentQueue<>();
    private int message_counter = 0;
    private String myself;
    private boolean first_message = true;

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo info = spreadMessage.getMembershipInfo();

        try {
            switch (info.getGroup().toString()) {

                case "Servers":
                    boolean from_myself = false;
                    if(spreadMessage.getSender().toString().equals(this.myself))
                        from_myself = true;
                    this.queue.add(new Triplet<>(from_myself, message_counter++, MessageOuterClass.Message.parseFrom(spreadMessage.getData())));
                    break;

            }
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo info = spreadMessage.getMembershipInfo();

        if (info.isCausedByJoin() && this.first_message) { // Someone joined the arena
            this.first_message = false;
            this.myself = info.getJoined().toString();
        }
    }

    public Triplet<Boolean, Integer, MessageOuterClass.Message> getMessage(){
        return this.queue.poll();
    }
}
