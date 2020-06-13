package server;

import middleware.gateway.Gateway;
import middleware.server.ServerMessageListener;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import spread.SpreadException;
import database.DatabaseManager;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static final long TMAX = 20; // Time to delete a cart in seconds

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

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

        // Wait for server to recover
        serverMessageListener.waitToRecover();

        // Initializing RequestManager and run it
        new Thread(RequestManager.initialize(serverMessageListener)).start();
        // Initializing ReplicationManager and run it
        new Thread(ReplicationManager.initialize(serverMessageListener)).start();

        // Initializing Gateway that delivers connection to a ClientManager Thread
        new Gateway(port, ClientManager.class);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            Runnable worker = new OnlineSuperMarketSkeleton();
            executor.execute(worker);
        }

    }
}