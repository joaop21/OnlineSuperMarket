package middleware;

import middleware.loadBalancer.LBSpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;

public class LBTester {
    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {
        new LBSpreadConnector();
        while(true)
            Thread.sleep(10000);
    }
}
