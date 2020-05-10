package client;

import middleware.proto.MessageOuterClass;
import middleware.proto.RequestOuterClass;
import middleware.socket.SocketIO;

import java.io.IOException;
import java.net.Socket;

public class test1 implements Runnable {

    @Override
    public void run() {
        try {
            // Starting socket
            Socket socket = new Socket("localhost", 9999);
            SocketIO socketIO = new SocketIO(socket);

            MessageOuterClass.Message message1 = MessageOuterClass.Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setGetItems(RequestOuterClass.GetItems.newBuilder().build())
                            .build())
                    .build();

            socketIO.write(message1.toByteArray());

            while (true) Thread.sleep(10000);

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for(int i = 0 ; i < 10 ; i++)
            new Thread(new test1()).start();
    }
}
