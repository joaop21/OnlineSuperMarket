package middleware.spread;

import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.UUID;

public class SpreadConnector {

    private static SpreadConnector spreadConnectorInstance = null;

    private SpreadConnection spreadConn = null;
    private String connName = null;

    private Set<String> groups;
    private AdvancedMessageListener messageListener;

    private SpreadConnector (Set<String> groups, AdvancedMessageListener messageListener) {

        this.groups = groups;
        this.messageListener = messageListener;

    }

    // Returns the SpreadConnector instance
    public static SpreadConnector SpreadConnector (Set<String> groups, AdvancedMessageListener messageListener) {

        if (spreadConnectorInstance == null)
            spreadConnectorInstance = new SpreadConnector(groups, messageListener);

        return spreadConnectorInstance;

    }

    // Returns null if SpreadConnector hasn't been started with groups and a AdvancedMessageListener
    public static SpreadConnector SpreadConnector () {

        return spreadConnectorInstance;

    }

    public void initializeConnector() throws UnknownHostException, SpreadException {

        if (spreadConn == null || connName == null) {

            spreadConn = new SpreadConnection();
            connName = UUID.randomUUID().toString();
            spreadConn.connect(InetAddress.getByName("localhost"), 4803, connName, false, true);

            // Setting what to do when info from spread is received
            spreadConn.add(this.messageListener);

            // Joining groups
            for (String group: this.groups) {

                SpreadGroup spreadGroup = new SpreadGroup();
                spreadGroup.join(spreadConn, group);

            }
        }

    }

    public void cast (byte[] message, Set<String> groups){

        SpreadMessage m = new SpreadMessage();
        m.addGroups(groups.toArray(new String[groups.size()]));
        m.setData(message);
        m.setSafe();

        try {

            if(spreadConn == null) initializeConnector();
            spreadConn.multicast(m);

        } catch (SpreadException | UnknownHostException e) {

            e.printStackTrace();

        }
    }

}
