package middleware.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketIO {

    private Socket socket = null;
    private OutputStream os;
    private InputStream in;

    public SocketIO (Socket socket) {

        this.socket = socket;
        try {
            this.os = this.socket.getOutputStream();
            this.in = this.socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] read () throws IOException {

        // 10kb
        byte[] len = new byte[10240];
        int count = 0;
        count = this.in.read(len);

        if (count < 0)
            throw new IOException("Socket Closed.");

        byte[] temp = new byte[count];
        System.arraycopy(len, 0, temp, 0, count);
        return temp;

    }

    public void write (byte[] data) throws IOException {

        this.os.write(data);

    }

}
