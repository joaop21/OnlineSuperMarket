package loadBalancer;

import middleware.loadBalancer.LBSpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;

public class LoadBalancer {
    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {
        LBSpreadConnector.initializeConnector();
        LBSpreadConnector.waitToBePrimary();
        System.out.println("I'm Primary!");
        // Because I'm primary now I can make the server selection
        while(true)
            Thread.sleep(10000);
    }
}
