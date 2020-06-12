package server;

import middleware.server.ConcurrentQueue;
import middleware.server.Pair;
import middleware.server.ServerMessageListener;
import middleware.server.Triplet;
import middleware.proto.MessageOuterClass.Message;
import middleware.spread.SpreadConnector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RequestManager implements Runnable {
    private static RequestManager instance = null;
    private static ServerMessageListener messageListener = null;
    /* Map of waiting rooms where clientManager threads are waiting for their request to be responded */
    private static Map<Pair<String,String>, WaitingRoom> waiting_requests = null;
    /* requests for primary to respond */
    private static ConcurrentQueue<Message> sorted_requests = null;
    /* Requests are stored in case primary goes down and three is requests to be attended */
    private static Map<Pair<String,String>, Pair<Long,Message>> secondary_backup = null;
    private static final Lock lock = new ReentrantLock();
    private static final Condition empty = lock.newCondition();

    public RequestManager(ServerMessageListener serverMessageListener){
        messageListener = serverMessageListener;
        waiting_requests = new ConcurrentHashMap<>();
        sorted_requests = new ConcurrentQueue<>();
        secondary_backup = new ConcurrentHashMap<>();
    }

    public static RequestManager initialize(ServerMessageListener serverMessageListener){
        if(instance == null)
            instance = new RequestManager(serverMessageListener);
        return instance;
    }


    @Override
    public void run() {

        Triplet<Boolean, Long, Message> message;

        while((message = messageListener.getNextRequest()) != null){

            Pair<String, String> pair = new Pair<>(message.getThird().getRequest().getSender(), message.getThird().getRequest().getUuid());

            // if i'm not the origin of the message
            if (!message.getFirst())
                waiting_requests.put(pair,new WaitingRoom());

            // if i'm primary i handle it
            if(messageListener.isPrimary()) {

                if(secondary_backup.size() != 0)
                    exchangeSecondaryBackupToSortedRequests();

                sorted_requests.add(message.getThird());

            } else {

                secondary_backup.put(pair, new Pair<>(message.getSecond(), message.getThird()));

            }

        }
    }

    public static Message publishRequest(Message msg){

        WaitingRoom wr = new WaitingRoom();
        wr.activate();

        String sender = messageListener.getMyself();
        String key = null;
        do{
            key = UUID.randomUUID().toString();
        } while(waiting_requests.putIfAbsent(new Pair<>(sender, key), wr) != null);

        Message.Builder builder = msg.toBuilder();
        builder.getRequestBuilder().setUuid(key).setSender(sender);
        msg = builder.build();

        SpreadConnector.cast(msg.toByteArray(), Set.of("Servers"));

        return wr.waitToProceed();
    }

    public static void putResponse(Pair<String,String> request_key, Message msg){

        try {

            // remove from waiting_requests
            WaitingRoom wr = waiting_requests.get(request_key);

            if(wr.isActive())
                wr.putMessage(msg);
            else
                waiting_requests.remove(request_key);

            // remove from secondary_backup
            secondary_backup.remove(request_key);

        } catch (NullPointerException e) {}

    }

    public static Message getNextRequest(){

        lock.lock();

        if(sorted_requests.size() == 0)
            empty.signal();

        lock.unlock();

        return sorted_requests.poll();

    }

    public static void waitToEmpty() {

        try {

            lock.lock();

            while(sorted_requests.size() != 0)
                empty.await();

            lock.unlock();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void exchangeSecondaryBackupToSortedRequests() {

        List<Pair<Long,Message>> values = (List<Pair<Long, Message>>) secondary_backup.values();
        values.sort(new PairComparator());
        values.forEach(l -> sorted_requests.add(l.getSecond()));

    }

    static class PairComparator implements Comparator<Pair<Long,Message>> {

        @Override
        public int compare(Pair<Long, Message> pair1, Pair<Long, Message> pair2) {
            return (int) (pair1.getFirst() - pair2.getFirst());
        }

    }

}
