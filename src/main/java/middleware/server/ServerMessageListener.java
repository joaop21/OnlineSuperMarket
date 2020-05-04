package middleware.server;

import spread.AdvancedMessageListener;
import spread.SpreadMessage;

public class ServerMessageListener implements AdvancedMessageListener {
    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        // Here we receive regular messages from Spread
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        // Here we receive membership messages from Spread
        String info = spreadMessage.getMembershipInfo().getGroup().toString();
        System.out.println(info);
    }
}
