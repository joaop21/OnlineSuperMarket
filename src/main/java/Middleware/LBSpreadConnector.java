package Middleware;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class LBSpreadConnector {
    private final String connName;
    private SpreadConnection conn;

    public LBSpreadConnector() throws UnknownHostException, SpreadException {
        this.conn = new SpreadConnection();
        this.connName = UUID.randomUUID().toString();
        this.conn.connect(InetAddress.getByName("localhost"), 4803, this.connName, false, true);

        //this.conn.add(new AdvListener());

        SpreadGroup g = new SpreadGroup();
        g.join(this.conn, "LoadBalancing");
    }
}
