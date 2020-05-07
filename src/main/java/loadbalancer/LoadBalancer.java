package loadbalancer;

import middleware.loadbalancer.LoadBalancerMessageListener;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class LoadBalancer {

    private static SpreadConnector spreadConnector;

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting own port
        int port = Integer.parseInt(args[0]);

        System.out.println("Creating Connector!");
        // Creating connector
        spreadConnector = new SpreadConnector(Set.of("LoadBalancing", "System"), new LoadBalancerMessageListener(port));
        System.out.println("Initializing Connector!");
        // Initializing connector
        spreadConnector.initializeConnector();
        System.out.println("Initialized Connector!");

        // Because I'm primary now I can make the server selection
        while(true) Thread.sleep(10000);
    }
}
