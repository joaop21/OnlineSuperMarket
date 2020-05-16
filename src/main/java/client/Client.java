package client;

import middleware.proto.MessageOuterClass.*;

import java.io.*;

public class Client {

    public static void main(String[] args) throws IOException {

        Message message = Message.newBuilder().build();

        ClientDriver.request(message);

        while (true) {

            try {

                ClientDriver.request(message);

                Thread.sleep(5000);

            } catch (InterruptedException ignored) {}

        }

    }
}
