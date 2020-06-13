package client;

import middleware.proto.MessageOuterClass.Message;
import middleware.socket.SocketIO;

import java.io.IOException;
import java.net.Socket;

public class ClientDriver {

    private static final int loadBalancerWaitTime = 1000;
    private static final int serverWaitTime = 1000;

    private static final int loadBalancerPort = 10000;

    private static Socket loadBalancerSocket = null;
    private static SocketIO loadBalancerSocketIO = null;

    private static Socket serverSocket = null;
    private static SocketIO serverSocketIO = null;

    private static void getLoadBalancer () {

        while (loadBalancerSocket == null) {

            // Starting socket with load balancer
            try {

                // Getting load balancer socket info
                ClientDriver.loadBalancerSocket = new Socket("localhost", ClientDriver.loadBalancerPort);
                ClientDriver.loadBalancerSocketIO = new SocketIO(ClientDriver.loadBalancerSocket);

            } catch (IOException e) {

                // Shutting down load balancer connection
                ClientDriver.shutdownLoadBalancerConnection();

                // Waiting 1s to try to connect again
                try { Thread.sleep(ClientDriver.loadBalancerWaitTime); } catch (InterruptedException ignored) {}

            }

        }

    }

    private static void getServer () {

        while (ClientDriver.serverSocket == null) {

            try {

                //System.out.println("Connecting to load balancer!");

                // Connecting to a load balancer
                getLoadBalancer();

                //System.out.println("Connected to load balancer!");

                // Receiving message
                Message message = Message.parseFrom(ClientDriver.loadBalancerSocketIO.read());

                //System.out.println("Received message from Load Balancer!");

                // Shutting down load balancer connection
                ClientDriver.shutdownLoadBalancerConnection();

                if (!message.getAssignment().hasError()) {

                    String address =message.getAssignment().getServerInfo().getAddress();
                    int port = message.getAssignment().getServerInfo().getPort();

                    //System.out.println("Server Info -> Address : " + address + " ; Port : " + port + " ;");

                    // Updating Socket info
                    ClientDriver.serverSocket = new Socket(address, port);
                    ClientDriver.serverSocketIO = new SocketIO(ClientDriver.serverSocket);

                } else {

                    // Error message from Load Balancer
                    //System.out.println("Couldn't retrieve Server Info: " +  message.getAssignment().getError().getType());

                    // Shutting down load balancer & server connection
                    ClientDriver.shutdownLoadBalancerConnection();
                    ClientDriver.shutdownServerConnection();

                    try { Thread.sleep(ClientDriver.serverWaitTime); } catch (InterruptedException ignored) {}

                }

            } catch (IOException e) { // Problems creating socket should not wait

                // Shutting down load balancer & server connection
                ClientDriver.shutdownLoadBalancerConnection();
                ClientDriver.shutdownServerConnection();

            }

        }

    }


    public static Message request (Message message) {

        //System.out.println("Connecting to server!");

        // Connecting to a server
        getServer();

        try {

            //System.out.println("Connected to server!");

            // Sending message to server
            ClientDriver.serverSocketIO.write(message.toByteArray());

            // Receiving message from server
            message = Message.parseFrom(ClientDriver.serverSocketIO.read());

            return message;

        } catch (IOException e) {

            e.printStackTrace();

            // Shutting down load balancer & server connection
            ClientDriver.shutdownLoadBalancerConnection();
            ClientDriver.shutdownServerConnection();

            // Requesting message again
            return ClientDriver.request(message);

        }

    }

    private static void shutdownLoadBalancerConnection () {

        if (ClientDriver.loadBalancerSocket != null) try { ClientDriver.loadBalancerSocket.close(); } catch (IOException ignored) {}

        ClientDriver.loadBalancerSocket = null;
        ClientDriver.loadBalancerSocketIO = null;

    }

    private static void shutdownServerConnection () {

        if (ClientDriver.serverSocket != null) try { ClientDriver.serverSocket.close(); } catch (IOException ignored) {}

        ClientDriver.serverSocket = null;
        ClientDriver.serverSocketIO = null;

    }

}
