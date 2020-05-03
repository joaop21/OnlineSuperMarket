package Middleware.Server;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Singleton class
 * Only one instance of this class in runtime
 * */
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

    public static void multicast(/* message */){
        SpreadMessage m = new SpreadMessage();
        m.addGroup("Servers");
        // m.setData(message.toByteArray());
        m.setSafe();
        try {
            if(spreadConn == null)
                initializeConnector();

            spreadConn.multicast(m);
        } catch (SpreadException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
