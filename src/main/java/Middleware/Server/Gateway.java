package middleware.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Gateway implements Runnable {
    private final int port;

    public Gateway(int port){
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket s;
        while (true) {
            try {
                assert ss != null;
                s = ss.accept();
                System.out.println("[SERVER - "+this.port+"]: Received new Connection");
                // new Thread(new Skeleton(this.bank,spconn,s)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
