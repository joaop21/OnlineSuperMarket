package server;

import middleware.gateway.Gateway;
import middleware.server.ServerMessageListener;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class Server {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("Servers", "System"));
        // Adding listener to connector
        SpreadConnector.addListener(new ServerMessageListener());
        // Initializing connector
        SpreadConnector.initialize();

        new Gateway(port, OnlineSuperMarketSkeleton.class);
        // Sleeping
        while(true) Thread.sleep(10000);
    }
}