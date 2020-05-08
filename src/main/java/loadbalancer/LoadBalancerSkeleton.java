package loadbalancer;

import middleware.gateway.Skeleton;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.socket.SocketIO;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LoadBalancerSkeleton extends Skeleton {

    private SocketIO socketIO = null;

    public LoadBalancerSkeleton (Socket sock) { super (sock); this.socketIO = new SocketIO(sock); }

    @Override
    public void run() {

            try {

                // Choosing a port
                ServerInfo server_info = (ServerInfo) Balancer.Balancer().get();

                // Marshalling the server info
                Message message = Message.newBuilder()
                        .setAssignment(Assignment.newBuilder()
                                .setServerInfo(server_info)
                                .build())
                        .build();

                System.out.println("Sending message to Client!");

                // Sending info to the client
                this.socketIO.write(message.toByteArray());

                // Closing connection
                this.socket.close();

            } catch (IOException e) {

                e.printStackTrace();

            }

        }


}
