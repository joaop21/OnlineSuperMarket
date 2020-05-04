package Server;

import Middleware.Server.ServerSpreadConnector;
import spread.SpreadException;

import java.net.UnknownHostException;

public class Server {
    public static void main(String[] args) throws SpreadException, UnknownHostException, InterruptedException {
        ServerSpreadConnector.initializeConnector();
        while(true)
            Thread.sleep(10000);
    }
}
