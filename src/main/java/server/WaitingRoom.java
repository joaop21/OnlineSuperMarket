package server;

import middleware.proto.MessageOuterClass.Message;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Auxiliary class that behaves as a monitor (synchronization).
 * */
public class WaitingRoom{
    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();
    private Message message = null;
    private boolean active = false;

    public Message waitToProceed(){
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

    void putMessage(Message msg){
        try {
            l.lock();
            this.message = msg;
            cond.signal();
        } finally {
            l.unlock();
        }
    }

    public boolean isActive() { return active; }

    public void activate(){this.active = true;}
}
