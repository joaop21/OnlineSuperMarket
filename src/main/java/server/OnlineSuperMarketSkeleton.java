package server;

import application.Item;
import application.OnlineSuperMarket;
import database.*;
import middleware.proto.MessageOuterClass.Message;
import middleware.proto.ReplicationOuterClass;
import middleware.spread.SpreadConnector;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OnlineSuperMarketSkeleton implements OnlineSuperMarket, Runnable {

    @Override
    public List<Item> getItems() {
        return QueryItem.getItems();
    }

    @Override
    public Item getItem(int itemId) {
        return QueryItem.getItem(itemId);
    }

    @Override
    public boolean addItemToCart(int userId, int itemId) {
        List<DatabaseModification> mods = QueryCart.addItemToCart(userId, itemId);
        // DatabaseManager.loadModifications(mods);
        return mods != null && !mods.isEmpty();
    }

    @Override
    public boolean removeItemFromCart(int userId, int itemId) {
        List<DatabaseModification> mods = QueryCart.removeItemFromCart(userId, itemId);
        // DatabaseManager.loadModifications(mods);
        return mods != null && !mods.isEmpty();
    }

    @Override
    public List getCartItems(int userId) { return QueryCart.getCartItems(userId); }

    @Override
    public boolean order(int userId) {
        List mods = QueryCart.order(userId);
        // DatabaseManager.loadModifications(mods);
        return mods != null && !mods.isEmpty();
    }

    @Override
    public int login(String username, String password) {
        return QueryCustomer.checkPassword(username, password);
    }

    @Override
    public void run() {

        Message msg;

        while((msg = RequestManager.getNextRequest()) != null){

            switch(msg.getRequest().getOperationCase()){

                case ADDITEMTOCART:
                    List<DatabaseModification> mods1 = QueryCart.addItemToCart(msg.getRequest().getAddItemToCart().getUserId(),
                            msg.getRequest().getAddItemToCart().getItemId());

                    assert mods1 != null;
                    SpreadConnector.cast(constructFromModifications(msg, mods1).toByteArray(), Set.of("Servers"));
                    break;

                case REMOVEITEMFROMCART:
                    List<DatabaseModification> mods2 = QueryCart.removeItemFromCart(msg.getRequest().getRemoveItemFromCart().getUserId(),
                            msg.getRequest().getAddItemToCart().getItemId());

                    assert mods2 != null;
                    SpreadConnector.cast(constructFromModifications(msg, mods2).toByteArray(), Set.of("Servers"));
                    break;

                case ORDER:
                    List<DatabaseModification> mods3 = QueryCart.order(msg.getRequest().getOrder().getUserId());

                    assert mods3 != null;
                    SpreadConnector.cast(constructFromModifications(msg, mods3).toByteArray(), Set.of("Servers"));
                    break;

            }
        }
    }

    public Message constructFromModifications(Message msg, List<DatabaseModification> mods){

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

        return Message.newBuilder()
                .setReplication(ReplicationOuterClass.Replication.newBuilder()
                        .setModifications(ReplicationOuterClass.DatabaseModifications.newBuilder()
                                .setStatus(!mods.isEmpty())
                                .setSender(msg.getRequest().getSender())
                                .setRequestUuid(msg.getRequest().getUuid())
                                .addAllModifications(modificationsprotos)
                                .build())
                        .build())
                .build();

    }

    private List<ReplicationOuterClass.DatabaseModifications.Modification.FieldValue> constructListFieldValues(List<FieldValue> list) {

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
