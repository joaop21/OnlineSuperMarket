package benchmarking;

import middleware.proto.MessageOuterClass.Message;
import middleware.socket.SocketIO;

import java.io.IOException;
import java.net.Socket;

public class Driver {

    private int loadBalancerWaitTime;
    private int serverWaitTime;

    private int loadBalancerPort;

    private Socket loadBalancerSocket;
    private SocketIO loadBalancerSocketIO = null;

    private Socket serverSocket;
    private SocketIO serverSocketIO;

    Driver(){
        this.loadBalancerWaitTime = 1000;
        this.serverWaitTime = 1000;
        this.loadBalancerPort = 10000;
        this.loadBalancerSocket = null;
        this.loadBalancerSocketIO = null;
        this.serverSocket = null;
        this.serverSocketIO = null;
    }

    private void getLoadBalancer () {

        while (loadBalancerSocket == null) {

            // Starting socket with load balancer
            try {

                // Getting load balancer socket info
                this.loadBalancerSocket = new Socket("localhost", this.loadBalancerPort);
                this.loadBalancerSocketIO = new SocketIO(this.loadBalancerSocket);

            } catch (IOException e) {

                // Shutting down load balancer connection
                this.shutdownLoadBalancerConnection();

                // Waiting 1s to try to connect again
                try { Thread.sleep(this.loadBalancerWaitTime); } catch (InterruptedException ignored) {}

            }

        }

    }

    private void getServer () {

        while (this.serverSocket == null) {

            try {

                //System.out.println("Connecting to load balancer!");

                // Connecting to a load balancer
                getLoadBalancer();

                //System.out.println("Connected to load balancer!");

                // Receiving message
                Message message = Message.parseFrom(this.loadBalancerSocketIO.read());

                //System.out.println("Received message from Load Balancer!");

                // Shutting down load balancer connection
                this.shutdownLoadBalancerConnection();

                if (!message.getAssignment().hasError()) {

                    String address =message.getAssignment().getServerInfo().getAddress();
                    int port = message.getAssignment().getServerInfo().getPort();

                    //System.out.println("Server Info -> Address : " + address + " ; Port : " + port + " ;");

                    // Updating Socket info
                    this.serverSocket = new Socket(address, port);
                    this.serverSocketIO = new SocketIO(this.serverSocket);

                } else {

                    // Error message from Load Balancer
                    //System.out.println("Couldn't retrieve Server Info: " +  message.getAssignment().getError().getType());

                    // Shutting down load balancer & server connection
                    this.shutdownLoadBalancerConnection();
                    this.shutdownServerConnection();

                    try { Thread.sleep(this.serverWaitTime); } catch (InterruptedException ignored) {}

                }

            } catch (IOException e) { // Problems creating socket should not wait

                // Shutting down load balancer & server connection
                this.shutdownLoadBalancerConnection();
                this.shutdownServerConnection();

            }

        }

    }


    public Message request (Message message) {

        //System.out.println("Connecting to server!");

        // Connecting to a server
        getServer();

        try {

            //System.out.println("Connected to server!");

            // Sending message to server
            this.serverSocketIO.write(message.toByteArray());

            // Receiving message from server
            message = Message.parseFrom(this.serverSocketIO.read());

            return message;

        } catch (IOException e) {

            // Shutting down load balancer & server connection
            this.shutdownLoadBalancerConnection();
            this.shutdownServerConnection();

            // Requesting message again
            return this.request(message);

        }

    }

    private void shutdownLoadBalancerConnection () {

        if (this.loadBalancerSocket != null) try { this.loadBalancerSocket.close(); } catch (IOException ignored) {}

        this.loadBalancerSocket = null;
        this.loadBalancerSocketIO = null;

    }

    private void shutdownServerConnection () {

        if (this.serverSocket != null) try { this.serverSocket.close(); } catch (IOException ignored) {}

        this.serverSocket = null;
        this.serverSocketIO = null;

    }

}