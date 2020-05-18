package server;

import application.Item;
import application.OnlineSuperMarket;
import database.QueryCustomer;
import database.QueryItem;
import middleware.proto.MessageOuterClass.Message;
import middleware.proto.ReplicationOuterClass;
import middleware.spread.SpreadConnector;

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
    public Item getItem(String itemName) {
        return null;
    }

    @Override
    public boolean addItemToCart(int userId, int itemId) {
        System.out.println(userId + " : " + itemId);
        return true;
    }

    @Override
    public void removeItemFromCart(int userId, int itemId) {

    }

    @Override
    public List<Item> getCartItems(int userId) {
        return null;
    }

    @Override
    public boolean order(int userId) {
        return false;
    }

    @Override
    public int login(String username, String password) {
        return -1;
    }

    @Override
    public void run() {

        Message msg;

        while((msg = RequestManager.getNextRequest()) != null){

            switch(msg.getRequest().getTypeCase()){

                case ADDITEMTOCART:
                    Message msg1 = Message.newBuilder()
                            .setReplication(ReplicationOuterClass.Replication.newBuilder()
                                    .setUpdates(ReplicationOuterClass.DatabaseUpdates.newBuilder()
                                            .setStatus(true)
                                            .setSender(msg.getRequest().getSender())
                                            .setRequestUuid(msg.getRequest().getUuid())
                                            .addModifications( ReplicationOuterClass.DatabaseUpdates.Modification.newBuilder()
                                                    .setTable("Cart_Item")
                                                    .setId(1)
                                                    .setField("Itemid")
                                                    .setValueInt(2)
                                                    .build())
                                            .build())
                                    .build())
                            .build();
                    SpreadConnector.cast(msg1.toByteArray(), Set.of("Servers"));
                    // send to db
                    // get and send replication message
                    break;

                case REMOVEITEMFROMCART:
                    // send to db
                    // get and send replication message
                    break;

                case ORDER:
                    // send to db
                    // get and send replication message
                    break;

            }
        }

    }
}
