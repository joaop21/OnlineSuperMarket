package Middleware.Server;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class ServerSpreadConnector {
    private static SpreadConnection spreadConn = null;
    private static String connName = null;

    public static void initializeConnector() throws UnknownHostException, SpreadException {
        if(spreadConn == null || connName == null) {
            spreadConn = new SpreadConnection();
            connName = UUID.randomUUID().toString();
            spreadConn.connect(InetAddress.getByName("localhost"), 4803, connName, false, true);

            spreadConn.add(new ServerMessageListener());

            // Group for application servers only
            SpreadGroup serversGroup = new SpreadGroup();
            serversGroup.join(spreadConn, "Servers");
            // Group for all servers in system (includes Load Balancers)
            SpreadGroup systemGroup = new SpreadGroup();
            systemGroup.join(spreadConn, "System");
        }
    }
}
