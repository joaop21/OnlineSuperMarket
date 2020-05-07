package middleware.gateway;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;

public class Gateway implements Runnable {
    private final int port;
    private Constructor<?> skeletonConstructer;

    public Gateway(int port, Class<?> skeletonClass){
        this.port = port;
        this.skeletonConstructer = null;
        if(Skeleton.class.isAssignableFrom(skeletonClass)){
            try {
                this.skeletonConstructer = skeletonClass.getConstructor(Socket.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            new Thread(this).start();
        } else {
            System.out.println("The passed parameter class must extend " + Skeleton.class);
            System.exit(1);
        }
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
                System.out.println("["+this.port+"]: Received new Connection");
                new Thread((Runnable) this.skeletonConstructer.newInstance(s)).start();
            } catch (IOException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
