package Middleware.LoadBalancer;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LBSpreadConnector {
    private final String connName;
    private SpreadConnection conn;
    private LBMessageListener lbml;
    private final Lock l = new ReentrantLock();
    private final Condition primaryCond = l.newCondition();

    public LBSpreadConnector() throws UnknownHostException, SpreadException {
        this.conn = new SpreadConnection();
        this.connName = UUID.randomUUID().toString();
        this.conn.connect(InetAddress.getByName("localhost"), 4803, this.connName, false, true);

        this.lbml = new LBMessageListener(this.l, this.primaryCond);
        this.conn.add(this.lbml);

        SpreadGroup g = new SpreadGroup();
        g.join(this.conn, "LoadBalancing");
    }

    public void waitToBePrimary(){
        try {
            this.l.lock();
            while(!this.lbml.getPrimary())
                this.primaryCond.await();
        } catch(InterruptedException e){
            e.printStackTrace();
        } finally {
            this.l.unlock();
        }
    }
}
