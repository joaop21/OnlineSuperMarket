package server;

import middleware.proto.MessageOuterClass.Message;
import middleware.server.Pair;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Auxiliary class that behaves as a monitor (synchronization).
 * */
public class WaitingRoom{
    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();
    private Pair<Long, Message> message = null;

    public Pair<Long, Message> waitToProceed(){
        try{
            l.lock();
            while(message == null)
                cond.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            l.unlock();
        }
        return message;
    }

    void putMessage(Pair<Long, Message> msg){
        try {
            l.lock();
            this.message = msg;
            cond.signal();
        } finally {
            l.unlock();
        }
    }
}
