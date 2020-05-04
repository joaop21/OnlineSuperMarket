package middleware.loadBalancer;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton class
 * Only one instance of this class in runtime
 * */
public class LBSpreadConnector {
    private static SpreadConnection spreadConn = null;
    private static String connName = null;
    private static LBMessageListener lbml = null;
    private static final Lock lock = new ReentrantLock();
    private static final Condition primaryCond = lock.newCondition();

    public static void initializeConnector() throws UnknownHostException, SpreadException {
        if (spreadConn == null && connName == null) {
            spreadConn = new SpreadConnection();
            connName = UUID.randomUUID().toString();
            spreadConn.connect(InetAddress.getByName("localhost"), 4803, connName, false, true);

            lbml = new LBMessageListener(lock, primaryCond);
            spreadConn.add(lbml);

            SpreadGroup loadBalancersGroup = new SpreadGroup();
            loadBalancersGroup.join(spreadConn, "LoadBalancing");
        }
    }

    public static void waitToBePrimary(){
        try {
            lock.lock();
            while(!lbml.getPrimary())
                primaryCond.await();
        } catch(InterruptedException e){
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
