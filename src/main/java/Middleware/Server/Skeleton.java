package middleware.server;

import java.net.Socket;

public abstract class Skeleton {
    protected Socket socket;

    public Skeleton(Socket sock){
        this.socket = sock;
    }
}
