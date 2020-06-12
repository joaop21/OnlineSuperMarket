package middleware.spread;

import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpreadConnector {

    private static SpreadConnection spreadConn = null;
    private static String connName = null;
    private static int port = 0;

    private static HashSet<String> groups = new HashSet<>();
    private static AdvancedMessageListener messageListener = null;

    public static void addGroup(String group) throws SpreadException {

        if (!SpreadConnector.groups.contains(group)) {

            // Storing group
            SpreadConnector.groups.add(group);

            // If connected join group
            if (spreadConn != null && connName != null) {

                // Joining group
                new SpreadGroup().join(spreadConn, group);

            }

        }

    }

    public static void addGroups (Set<String> groups) throws SpreadException {

        // Joining groups
        for (String group: groups)

            if (!SpreadConnector.groups.contains(group)) {

                // Storing group
                SpreadConnector.groups.add(group);

                // If connected join group
                if (spreadConn != null && connName != null) {

                    // Joining group
                    new SpreadGroup().join(spreadConn, group);

                }

            }

    }

    public static void addListener (AdvancedMessageListener messageListener) {

        if (spreadConn != null && connName != null) {

            // Adding new message listener
            spreadConn.add(messageListener);

            // Removing old message listener
            spreadConn.remove(SpreadConnector.messageListener);

            // Changing stored message listener
            SpreadConnector.messageListener = messageListener;


        } else SpreadConnector.messageListener = messageListener;

    }

    public static void initialize(int port) throws UnknownHostException, SpreadException {

        initialization(String.valueOf(port));

    }

    public static void initialize() throws UnknownHostException, SpreadException {

        initialization(UUID.randomUUID().toString());

    }

    private static void initialization(String connName) throws UnknownHostException, SpreadException {

        if (SpreadConnector.spreadConn == null || SpreadConnector.connName == null) {

            // Creating connection
            SpreadConnector.spreadConn = new SpreadConnection();
            // Getting unique ID for the connection
            SpreadConnector.connName = connName;
            // Connecting
            SpreadConnector.spreadConn.connect(InetAddress.getByName("localhost"), 4803, connName, false, true);

            // Adding message listener
            SpreadConnector.spreadConn.add(SpreadConnector.messageListener);

            // Joining groups
            for (String group: SpreadConnector.groups) new SpreadGroup().join(SpreadConnector.spreadConn, group);

        }

    }

    // Cast to every joined group
    public static void cast (byte[] message) {

        SpreadMessage m = new SpreadMessage();
        m.addGroups(SpreadConnector.groups.toArray(new String[groups.size()]));
        m.setData(message);
        m.setSafe();

        try {

            if(SpreadConnector.spreadConn == null) SpreadConnector.initialize(port);
            SpreadConnector.spreadConn.multicast(m);

        } catch (SpreadException | UnknownHostException e) {

            e.printStackTrace();

        }

    }

    // Cast to specific groups
    public static void cast (byte[] message, Set<String> groups){

        SpreadMessage m = new SpreadMessage();
        m.addGroups(groups.toArray(new String[groups.size()]));
        m.setData(message);
        m.setSafe();

        try {

            if(SpreadConnector.spreadConn == null) SpreadConnector.initialize(port);
            SpreadConnector.spreadConn.multicast(m);

        } catch (SpreadException | UnknownHostException e) {

            e.printStackTrace();

        }
    }

    // Cast to specific groups
    public static void send (byte[] message, SpreadGroup group){

        SpreadMessage m = new SpreadMessage();
        m.addGroup(group);
        m.setData(message);
        m.setSafe();

        try {

            if(SpreadConnector.spreadConn == null) SpreadConnector.initialize(port);
            SpreadConnector.spreadConn.multicast(m);

        } catch (SpreadException | UnknownHostException e) {

            e.printStackTrace();

        }
    }

    // Unicast to specific member
    public static void unicast (byte[] message, String member){

        SpreadMessage m = new SpreadMessage();
        m.addGroup(member);
        m.setData(message);
        m.setReliable();

        try {

            if(SpreadConnector.spreadConn == null) SpreadConnector.initialize(port);
            SpreadConnector.spreadConn.multicast(m);

        } catch (SpreadException | UnknownHostException e) {

            e.printStackTrace();

        }
    }

}
