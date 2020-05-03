package Middleware.Server;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class ServerSpreadConnector {
    final String connName;
    SpreadConnection conn;

    public ServerSpreadConnector() throws UnknownHostException, SpreadException {
        this.conn = new SpreadConnection();
        this.connName = UUID.randomUUID().toString();
        this.conn.connect(InetAddress.getByName("localhost"), 4803, this.connName, false, true);

        this.conn.add(new ServerMessageListener());

        SpreadGroup g = new SpreadGroup();
        // Group for application servers only
        g.join(this.conn, "Servers");
        // Group for all servers in system (includes Load Balancers)
        g.join(this.conn, "System");
    }
}
