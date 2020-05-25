package server;

import database.DatabaseManager;
import database.DatabaseModification;
import database.FieldValue;
import database.ValueType;
import middleware.proto.MessageOuterClass.Message;
import middleware.proto.ReplicationOuterClass;
import middleware.proto.ReplicationOuterClass.Replication;
import middleware.server.Pair;
import middleware.server.ServerMessageListener;
import middleware.server.Triplet;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
            Pair<String,String> pair = new Pair<>(repl.getModifications().getSender(), repl.getModifications().getRequestUuid());
            RequestManager.putResponse(pair, msg.getThird());

            if(!msg.getFirst()){

                // System.out.println(repl);
                // System.out.println("It misses update the db");

                List<DatabaseModification> modifs = constructModification(repl.getModifications());
                DatabaseManager.loadModifications(modifs);

            } /*else {

                System.out.println("replication from myself: " + repl);

            }*/

        }

    }

    private List<DatabaseModification> constructModification(ReplicationOuterClass.DatabaseModifications modifications) {

        List<DatabaseModification> res = new ArrayList<>();

        List<ReplicationOuterClass.DatabaseModifications.Modification> modifs = modifications.getModificationsList();

        for(ReplicationOuterClass.DatabaseModifications.Modification mod : modifs) {

            List<FieldValue> mods = constructValueTypes(mod.getModsList());
            List<FieldValue> where = constructValueTypes(mod.getWhereList());

            res.add(new DatabaseModification(mod.getType(), mod.getTable(), mods, where));

        }

        return res;

    }

    private List<FieldValue> constructValueTypes(List<ReplicationOuterClass.DatabaseModifications.Modification.FieldValue> values) {

        List<FieldValue> res = new ArrayList<>();

        for(ReplicationOuterClass.DatabaseModifications.Modification.FieldValue fv : values) {

            Object value = null;
            ValueType vt = null;

            switch (fv.getType()) {

                case INTEGER:
                    value = fv.getValueInt();
                    vt = ValueType.INTEGER;
                    break;

                case STRING:
                    value = fv.getValueString();
                    vt = ValueType.STRING;
                    break;

                case BOOLEAN:
                    value = fv.getValueBool();
                    vt = ValueType.BOOLEAN;
                    break;

                case TIMESTAMP:
                    value = new Timestamp(fv.getValueTimestamp());
                    vt = ValueType.TIMESTAMP;
                    break;

                case NULL:
                    value = null;
                    vt = ValueType.NULL;
                    break;

            }

            res.add(new FieldValue(fv.getField(), value, vt));

        }

        return res;

    }

}
