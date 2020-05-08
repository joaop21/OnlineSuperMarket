package client;

import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.socket.SocketIO;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Client {

    private static Socket socket = null;
    private static SocketIO socketIO = null;

    private static int getPortFromLoadBalancer () throws IOException {


        Message message = Message.newBuilder()
                .setAssignment(
                        Assignment.newBuilder().build()
                ).build();

        Client.socketIO.write(message.toByteArray());

        System.out.println("Sent message to Load Balancer!");

        // Receiving message
        message = Message.parseFrom(Client.socketIO.read());

        System.out.println("Received message from Load Balancer!");

        // Returning assigned server port
        return message.getAssignment().getPort();

    }

    public static void main(String[] args) throws IOException {

        // Getting load balancer port
        int load_balancer_port = Integer.parseInt(args[0]);

        // Starting socket
        Client.socket = new Socket("localhost", load_balancer_port);
        Client.socketIO = new SocketIO(Client.socket);

        // Getting server port
        int server_port = getPortFromLoadBalancer();

        System.out.println("Server port: " + server_port);

        //System.out.println(InetAddress.getLocalHost());
        //System.out.println(InetAddress.getLoopbackAddress());

        Client.socket.close();
    }
}
