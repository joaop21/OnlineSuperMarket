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

            MessageOuterClass.Message message2 = MessageOuterClass.Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setGetItem(RequestOuterClass.GetItem.newBuilder().setItemId(1).build())
                            .build())
                    .build();

            MessageOuterClass.Message message3 = MessageOuterClass.Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setLogin(RequestOuterClass.Login.newBuilder().setUsername("joao").setPassword("joao").build())
                            .build())
                    .build();

            socketIO.write(message3.toByteArray());

            while (true)
                System.out.println(MessageOuterClass.Message.parseFrom(socketIO.read()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for(int i = 0 ; i < 1 ; i++)
            new Thread(new test1()).start();
    }
}
