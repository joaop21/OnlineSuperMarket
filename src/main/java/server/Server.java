package server;

import middleware.gateway.Gateway;
import middleware.server.ServerMessageListener;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import spread.SpreadException;
import database.DatabaseManager;

import java.net.UnknownHostException;
import java.util.Set;

public class Server {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

        DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/" + port + "/onlinesupermarket");

        // Setting socket info
        SocketInfo serverInfo = new SocketInfo("localhost", port);

        // Creating Server Message Listener
        ServerMessageListener serverMessageListener = new ServerMessageListener(serverInfo);

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("Servers", "System"));
        // Adding listener to connector
        SpreadConnector.addListener(serverMessageListener);
        // Initializing connector
        SpreadConnector.initialize(port);

        // Initializing RequestManager and run it
        new Thread(RequestManager.initialize(serverMessageListener)).start();
        // Initializing ReplicationManager and run it
        new Thread(ReplicationManager.initialize(serverMessageListener)).start();

        // Initializing Gateway that delivers connection to a ClientManager Thread
        new Gateway(port, ClientManager.class);

        new Thread(new OnlineSuperMarketSkeleton()).run();
    }
}