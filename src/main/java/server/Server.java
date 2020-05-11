package server;

import middleware.gateway.Gateway;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.server.ServerMessageListener;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class Server {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

        // Setting server info
        ServerInfo serverInfo = ServerInfo.newBuilder()
                .setAddress("localhost")
                .setPort(port)
                .build();

        // Creating Server Message Listener
        ServerMessageListener serverMessageListener = new ServerMessageListener(serverInfo);

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("Servers", "System"));
        // Adding listener to connector
        SpreadConnector.addListener(new ServerMessageListener(serverInfo));
        // Initializing connector
        SpreadConnector.initialize();

        new Thread(Orderer.initialize(serverMessageListener)).start();

        new Gateway(port, OnlineSuperMarketSkeleton.class);

        // Sleeping
        while(true) Thread.sleep(10000);
    }
}