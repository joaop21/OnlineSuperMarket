package server;

import application.Item;
import database.QueryItem;
import middleware.gateway.Gateway;
import middleware.proto.AssignmentOuterClass;
import middleware.proto.MessageOuterClass;
import middleware.server.ServerMessageListener;
import middleware.spread.SpreadConnector;
import spread.SpreadException;
import database.DatabaseManager;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;

public class Server {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting server port from args[0]
        int port = Integer.parseInt(args[0]);

        DatabaseManager.createDatabase(DatabaseManager.DB_URL);

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("Servers", "System"));
        // Adding listener to connector
        ServerMessageListener sml = new ServerMessageListener();
        SpreadConnector.addListener(sml);
        // Initializing connector
        SpreadConnector.initialize();

        // Creating message with own info to send to load balancer
        MessageOuterClass.Message message = MessageOuterClass.Message.newBuilder()
                .setAssignment(AssignmentOuterClass.Assignment.newBuilder()
                        .setServerInfo(AssignmentOuterClass.ServerInfo.newBuilder()
                                .setAddress("localhost")
                                .setPort(port)
                                .build())
                        .build())
                .build();

        // Sending own info throughout the system
        SpreadConnector.cast(message.toByteArray(), Set.of("System"));

        new Thread(Orderer.initialize(sml)).start();

        new Gateway(port, OnlineSuperMarketSkeleton.class);

        // Sleeping
        while(true) Thread.sleep(10000);
    }
}