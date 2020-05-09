package middleware.server;

import com.google.protobuf.InvalidProtocolBufferException;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.spread.SpreadConnector;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadMessage;

import java.util.Set;

public class ServerMessageListener implements AdvancedMessageListener {

    private ServerInfo serverInfo;

    public ServerMessageListener (ServerInfo serverInfo) { this.serverInfo = serverInfo; }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        try {

            Message message = Message.parseFrom(spreadMessage.getData());

            if (message.hasAssignment()) return; // Ignoring Assignment Messages sent from other servers

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

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

    public void handleSystemInfo (MembershipInfo info) {

        if (info.isCausedByJoin()) {

            // Creating message with own info to send to laod balancer
            Message message = Message.newBuilder()
                    .setAssignment(Assignment.newBuilder()
                            .setServerInfo(serverInfo)
                            .build())
                    .build();

            // Sending own info throughout the system
            SpreadConnector.cast(message.toByteArray(), Set.of("System"));

        }

    }

    public void handleServerInfo (MembershipInfo info) {}
}
