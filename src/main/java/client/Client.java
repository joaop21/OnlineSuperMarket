package client;

import proto.MessageOuterClass.Message;
import proto.AssignmentOuterClass.Assignment;

import java.io.IOException;
import java.net.Socket;

public class Client {

    private static int getPortFromLoadBalancer () throws IOException {

        // Starting socket
        Socket s = new Socket("localhost", 10000);

        // Receiving message
        Message message = Message.parseFrom(s.getInputStream().readAllBytes());

        // Closing socket
        s.close();

        // Getting port
        System.out.println("Port: " + message.getAssignment().getPort());

        return message.getAssignment().getPort();

    }

    public static void main(String[] args) throws IOException {

        getPortFromLoadBalancer();

    }
}
