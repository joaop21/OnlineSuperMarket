package client;

import middleware.proto.MessageOuterClass;
import middleware.proto.RequestOuterClass;
import middleware.socket.SocketIO;

import java.io.IOException;
import java.net.Socket;

public class test1 implements Runnable {
    private final int process_number;

    public test1(int i) {
        process_number = i;
    }

    @Override
    public void run() {
        try {
            // Starting socket
            int server_port = (int)Math.round(Math.random()*4) + 9996;
            Socket socket = new Socket("localhost", server_port);
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

            MessageOuterClass.Message message4 = MessageOuterClass.Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setAddItemToCart(RequestOuterClass.AddItemToCart.newBuilder()
                                    .setUserId(1)
                                    .setItemId(2)
                                    .build())
                            .build())
                    .build();

            socketIO.write(message4.toByteArray());

            MessageOuterClass.Message msg = MessageOuterClass.Message.parseFrom(socketIO.read());

            System.out.println("\n" + process_number + ":\n" + msg);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for(int i = 0 ; i < 100 ; i++)
            new Thread(new test1(i)).start();
    }
}
