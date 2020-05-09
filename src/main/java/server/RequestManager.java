package server;

import middleware.proto.MessageOuterClass;
import middleware.server.Pair;
import middleware.server.ServerMessageListener;
import middleware.server.Triplet;
import middleware.spread.SpreadConnector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Auxiliary class that behaves as a monitor
 * */
class WaitingRoom{
    private final Lock l = new ReentrantLock();
    private final Condition cond = l.newCondition();
    private Pair<Integer, MessageOuterClass.Message> message = null;

    public Pair<Integer, MessageOuterClass.Message> waitToProceed(){
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

    void putMessage(Pair<Integer, MessageOuterClass.Message> msg){
        try {
            l.lock();
            this.message = msg;
            cond.signal();
        } finally {
            l.unlock();
        }
    }
}

public class RequestManager implements Runnable {
    private static  RequestManager instance = null;
    private static ServerMessageListener sml = null;
    private static Map<String, WaitingRoom> waiting_requests = null;

    public RequestManager(ServerMessageListener serverMessageListener){
        sml = serverMessageListener;
        waiting_requests = new ConcurrentHashMap<>();
    }

    public static RequestManager initialize(ServerMessageListener serverMessageListener){
        if(instance == null) {
            instance = new RequestManager(serverMessageListener);
        }
        return instance;
    }

    public static boolean removeRequest(String key){
        return waiting_requests.remove(key) != null;
    }

    public Pair<Integer, MessageOuterClass.Message> waitToProceed(MessageOuterClass.Message msg){
        WaitingRoom wr = new WaitingRoom();
        String key = null;
        do{
            key = UUID.randomUUID().toString();
        } while(waiting_requests.putIfAbsent(key, wr) != null);

        msg.getRequest().toBuilder().setUuid(key).build();
        SpreadConnector.cast(msg.toByteArray(), Set.of("Servers"));

        return wr.waitToProceed();
    }

    @Override
    public void run() {
        while(true){
            Triplet<Boolean, Integer, MessageOuterClass.Message> message = sml.getMessage();
            if(message.getFirst()) {
                waiting_requests.get(message.getThird().getRequest().getUuid())
                        .putMessage(new Pair<>(message.getSecond(), message.getThird()));
            } else {
                // deliver to a new thread
            }
        }
    }
}
