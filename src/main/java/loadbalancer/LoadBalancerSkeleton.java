package loadbalancer;

import middleware.gateway.Skeleton;
import middleware.proto.AssignmentOuterClass;
import middleware.proto.MessageOuterClass;

import java.io.IOException;
import java.net.Socket;

public class LoadBalancerSkeleton extends Skeleton {

    public LoadBalancerSkeleton (Socket sock) { super (sock);}

    @Override
    public void run() {

        // Choosing a port
        int port = PortManager.PortManager().getServerPort();

        // Marshalling the port
        MessageOuterClass.Message message = MessageOuterClass.Message.newBuilder()
                .setAssignment(
                        AssignmentOuterClass.Assignment.newBuilder()
                                .setPort(port)
                                .build()
                )
                .build();

        try {

            System.out.println("Sending message!");

            // Sending port back
            this.socket.getOutputStream().write(message.toByteArray());
            this.socket.getOutputStream().flush();

            // Closing connection
            this.socket.close();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}
