package loadbalancer;

import middleware.gateway.Skeleton;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.socket.SocketIO;
import middleware.spread.SpreadConnector;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class LoadBalancerSkeleton extends Skeleton {

    private SocketIO socketIO = null;

    public LoadBalancerSkeleton (Socket sock) { super (sock); this.socketIO = new SocketIO(sock); }

    @Override
    public void run() {

        while (!this.socket.isClosed()) {

            try {

                // Message from client
                Message message = Message.parseFrom(this.socketIO.read());

                System.out.println("Received message from client!");

                // Choosing a port
                int port = PortManager.PortManager().getServerPort();

                // Marshalling the port
                message = message.toBuilder().
                        setAssignment(
                                message.getAssignment().toBuilder().
                                        setPort(port).
                                        build()).
                        build();


                System.out.println("Sending message to Client!");

                // Sending port back
                this.socketIO.write(message.toByteArray());

                // Closing connection
                this.socket.close();

                System.out.println("Sending message to other Load Balancers!");

                System.out.println("Socket: " + this.socket.toString());

                SpreadConnector.cast(message.toByteArray(), Set.of("LoadBalancing"));

            } catch (IOException e) {

                e.printStackTrace();

            }


        }

    }

}
