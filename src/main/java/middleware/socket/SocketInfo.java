package middleware.socket;

import java.util.Objects;

public class SocketInfo {

    private String address;
    private Integer port;

    public SocketInfo (String address, Integer port) {

        this.address = address;
        this.port = port;

    }

    public String getAddress() { return address; }

    public Integer getPort() { return port; }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocketInfo that = (SocketInfo) o;
        return address.equals(that.address) &&
                port.equals(that.port);

    }

    @Override
    public int hashCode() { return Objects.hash(address, port); }
}
