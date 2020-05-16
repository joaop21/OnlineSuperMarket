package server;

import middleware.proto.MessageOuterClass.Message;
import middleware.proto.ReplicationOuterClass.Replication;
import middleware.server.Pair;
import middleware.server.ServerMessageListener;
import middleware.server.Triplet;

public class ReplicationManager implements Runnable {
    private static ReplicationManager instance = null;
    private static ServerMessageListener messageListener = null;

    public ReplicationManager(ServerMessageListener sml) {
        messageListener = sml;
    }

    public static ReplicationManager initialize(ServerMessageListener serverMessageListener){
        if(instance == null)
            instance = new ReplicationManager(serverMessageListener);
        return instance;
    }

    @Override
    public void run() {

        Triplet<Boolean,Long,Message> msg;

        while((msg = messageListener.getNextReplication()) != null){

            Replication repl = msg.getThird().getReplication();
            Pair<String,String> pair = new Pair<>(repl.getUpdates().getSender(), repl.getUpdates().getRequestUuid());
            RequestManager.putResponse(pair, msg.getThird());

            if(!msg.getFirst()){

                System.out.println(repl);
                System.out.println("It misses update the db");

            } else {

                System.out.println("replication from myself: " + repl);

            }

        }

    }

}
