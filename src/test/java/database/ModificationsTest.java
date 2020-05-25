package database;

import middleware.proto.MessageOuterClass;
import middleware.proto.ReplicationOuterClass;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ModificationsTest {

    public static void main(String[] args) {

        DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/9999/onlinesupermarket");

        List<DatabaseModification> mods = QueryCart.addItemToCart(0, 0);

        assert mods != null;
        MessageOuterClass.Message msg = constructFromModifications(mods);
        System.out.println(msg);

    }

    public static MessageOuterClass.Message constructFromModifications(List<DatabaseModification> mods){

        List<ReplicationOuterClass.DatabaseModifications.Modification> modificationsprotos = new ArrayList<>();

        for (DatabaseModification modification : mods ){

            ReplicationOuterClass.DatabaseModifications.Modification mod =
                    ReplicationOuterClass.DatabaseModifications.Modification.newBuilder()
                            .setType(modification.getType())
                            .setTable(modification.getTable())
                            .addAllMods(constructListFieldValues(modification.getMods()))
                            .addAllWhere(constructListFieldValues(modification.getWhere()))
                            .build();

            modificationsprotos.add(mod);

        }

        return MessageOuterClass.Message.newBuilder()
                .setReplication(ReplicationOuterClass.Replication.newBuilder()
                        .setModifications(ReplicationOuterClass.DatabaseModifications.newBuilder()
                                .setStatus(!mods.isEmpty())
                                .setSender("sender")
                                .setRequestUuid("uuid")
                                .addAllModifications(modificationsprotos)
                                .build())
                        .build())
                .build();

    }

    private static List<ReplicationOuterClass.DatabaseModifications.Modification.FieldValue> constructListFieldValues(List<FieldValue> list) {

        List<ReplicationOuterClass.DatabaseModifications.Modification.FieldValue> res = new ArrayList<>();

        for(FieldValue fv : list) {

            ReplicationOuterClass.DatabaseModifications.Modification.FieldValue fvproto =
                    ReplicationOuterClass.DatabaseModifications.Modification.FieldValue.newBuilder()
                            .setField(fv.getField())
                            .build();

            switch(fv.getType()){

                case INTEGER:
                    fvproto = fvproto.toBuilder()
                            .setType(ReplicationOuterClass.DatabaseModifications.Modification.Type.INTEGER)
                            .setValueInt((Integer) fv.getValue())
                            .build();
                    break;

                case STRING:
                    fvproto = fvproto.toBuilder()
                            .setType(ReplicationOuterClass.DatabaseModifications.Modification.Type.STRING)
                            .setValueString((String) fv.getValue())
                            .build();
                    break;

                case BOOLEAN:
                    fvproto = fvproto.toBuilder()
                            .setType(ReplicationOuterClass.DatabaseModifications.Modification.Type.BOOLEAN)
                            .setValueBool((Boolean) fv.getValue())
                            .build();
                    break;

                case TIMESTAMP:
                    fvproto = fvproto.toBuilder()
                            .setType(ReplicationOuterClass.DatabaseModifications.Modification.Type.TIMESTAMP)
                            .setValueTimestamp(((Timestamp) fv.getValue()).getTime())
                            .build();
                    break;

                case NULL:
                    fvproto = fvproto.toBuilder()
                            .setType(ReplicationOuterClass.DatabaseModifications.Modification.Type.NULL)
                            .build();
                    break;

            }

            res.add(fvproto);

        }

        return res;

    }

}
