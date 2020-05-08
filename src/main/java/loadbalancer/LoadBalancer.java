package loadbalancer;

import middleware.loadbalancer.LoadBalancerMessageListener;
import middleware.proto.AssignmentOuterClass.*;
import middleware.spread.SpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Set;

public class LoadBalancer {

    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {

        // Getting own port
        int port = Integer.parseInt(args[0]);

        System.out.println("Configuring Connector!");

        // Adding groups to connector
        SpreadConnector.addGroups(Set.of("LoadBalancing", "System"));
        // Adding listener to connector
        SpreadConnector.addListener(new LoadBalancerMessageListener(
                LoadBalancerInfo.newBuilder()
                        .setAddress("localhost")
                        .setPort(port)
                        .build()));

        System.out.println("Initializing Connector!");
        // Initializing connector
        SpreadConnector.initialize();

        System.out.println("Initialized Connector!");

        // Because I'm primary now I can make the server selection
        while(true) Thread.sleep(10000);
    }
}
