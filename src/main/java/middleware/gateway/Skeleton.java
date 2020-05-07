package middleware.gateway;

import java.net.Socket;

public abstract class Skeleton implements Runnable {
    protected Socket socket;

    public Skeleton(Socket sock){
        this.socket = sock;
    }
}
