package middleware.socket;

import java.io.IOException;
import java.net.Socket;

public class SocketIO {

    private Socket socket = null;

    public SocketIO (Socket socket) {

        this.socket = socket;

    }

    public byte[] read () throws IOException {

        // Getting length of data
        int len = this.socket.getInputStream().read();

        // Throwing exception when Socket is closed
        if (len == -1) throw new IOException("Socket Closed.");

        // Creating buffer of said size (DANGEROUS!)
        byte[] data = new byte[len];

        // Reading to buffer
        if (this.socket.getInputStream().read(data) == -1)
            throw new IOException("Socket Closed.");

        return data;

    }

    public void write (byte[] data) throws IOException {

        // Writing the length of the data
        this.socket.getOutputStream().write(data.length);
        // Writing data
        this.socket.getOutputStream().write(data);
        // Flushing data
        this.socket.getOutputStream().flush();

    }

}
