package server;

import application.Item;
import application.OnlineSuperMarket;
import database.QueryCart;
import database.QueryCustomer;
import database.QueryItem;
import middleware.proto.MessageOuterClass.Message;
import middleware.proto.ReplicationOuterClass;
import middleware.spread.SpreadConnector;

import javax.management.Query;
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
    public boolean addItemToCart(int userId, int itemId) { return QueryCart.addItemToCart(userId, itemId); }

    @Override
    public boolean removeItemFromCart(int userId, int itemId) { return QueryCart.removeItemFromCart(userId, itemId); }

    @Override
    public List<Item> getCartItems(int userId) { return QueryCart.getCartItems(userId); }

    @Override
    public boolean order(int userId) {
        return QueryCart.order(userId);
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
