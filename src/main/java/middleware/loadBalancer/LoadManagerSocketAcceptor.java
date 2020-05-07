package middleware.loadBalancer;

import proto.AssignmentOuterClass.Assignment;
import proto.MessageOuterClass.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class    LoadManagerSocketAcceptor implements Runnable {

    private int port = 10000;
    private HashMap<Integer, Integer> serverPorts = new HashMap<>(Map.of(
            10001, 0,
            10002, 0,
            10003, 0));

    @Override
    public void run() {

        try {

            ServerSocket ss = new ServerSocket(port);

            do {

                // Accepting a client
                Socket s = ss.accept();

                // Choosing a port
                int port = getServerPort();

                // Marshalling the port
                Message message = Message.newBuilder()
                        .setAssignment(
                                Assignment.newBuilder()
                                .setPort(port)
                                .build()
                        )
                        .build();

                // Sending port back
                System.out.println("Sending message!");
                s.getOutputStream().write(message.toByteArray());
                s.getOutputStream().flush();

                // Closing connection
                s.close();

            } while (true);


        } catch (IOException e) {

            e.printStackTrace();

        }


    }

    private int getServerPort () {

        Map.Entry<Integer, Integer> chosen_entry = null;
        for (Map.Entry<Integer, Integer> entry: serverPorts.entrySet())
            if (chosen_entry == null || entry.getValue() < chosen_entry.getValue())
                chosen_entry = entry;

        serverPorts.put(chosen_entry.getKey(), chosen_entry.getValue() + 1);

        return chosen_entry.getKey();

    }

}
