package client;

import middleware.proto.MessageOuterClass.*;
import middleware.socket.SocketIO;

import java.io.*;
import java.net.Socket;

public class Client {

    private static Socket socket = null;
    private static SocketIO socketIO = null;

    private static void getServer () throws IOException {

        // Receiving message
        Message message = Message.parseFrom(Client.socketIO.read());

        System.out.println("Received message from Load Balancer!");

        if (message.getAssignment().hasError()) {

            System.out.println("Couldn't retrieve Server Info: " +  message.getAssignment().getError().getType());

        } else {

            System.out.println("Server Info -> Address : " + message.getAssignment().getServerInfo().getAddress() +
                    " ; Port : " + message.getAssignment().getServerInfo().getPort() + " ;");

            // Creating socket with server
            Socket serverSocket = new Socket(message.getAssignment().getServerInfo().getAddress(),
                    message.getAssignment().getServerInfo().getPort());

            // Closing Load Balancer Socket
            socket.close();

            // Updating Socket info
            Client.socket = serverSocket;
            Client.socketIO = new SocketIO(Client.socket);

        }

    }

    public static void main(String[] args) throws IOException {

        // Getting load balancer port
        int load_balancer_port = Integer.parseInt(args[0]);

        // Starting socket
        Client.socket = new Socket("localhost", load_balancer_port);
        Client.socketIO = new SocketIO(Client.socket);

        // Connecting to Server
        Client.getServer();

        // Closing socket
        Client.socket.close();
    }
}
