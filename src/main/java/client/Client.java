package client;

import middleware.proto.MessageOuterClass.Message;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private static int getPortFromLoadBalancer (int load_balancer_port) throws IOException {

        // Starting socket
        Socket s = new Socket("localhost", load_balancer_port);

        // Receiving message
        Message message = Message.parseFrom(s.getInputStream().readAllBytes());

        // Closing socket
        s.close();

        // Returning assigned server port
        return message.getAssignment().getPort();

    }

    public static void main(String[] args) throws IOException {

        // Getting load balancer port
        int load_balancer_port = Integer.parseInt(args[0]);

        // Getting server port
        int server_port = getPortFromLoadBalancer(load_balancer_port);

        System.out.println("Server port: " + server_port);

    }
}
