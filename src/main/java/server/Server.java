package server;

import middleware.server.Gateway;
import middleware.server.ServerMessageListener;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class Server {

    private static SpreadConnector spreadConnector;

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Creating connector
        spreadConnector = new SpreadConnector(Set.of("Servers", "System"), new ServerMessageListener());
        // Initializing connector
        spreadConnector.initializeConnector();
        // new Gateway(9999, Sample.class);
        // Sleeping
        while(true) Thread.sleep(10000);
    }
}