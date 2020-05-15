package server;

import application.Item;
import application.OnlineSuperMarket;
import database.QueryCustomer;
import database.QueryItem;
import middleware.proto.MessageOuterClass.Message;

import java.util.List;

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
    public boolean addItemToCart(String username, int itemId) {
        System.out.println(username + " : " + itemId);
        return true;
    }

    @Override
    public void removeItemFromCart(String username, int itemId) {

    }

    @Override
    public List<Item> getCartItems(String username) {
        return null;
    }

    @Override
    public boolean order(String username) {
        return false;
    }

    @Override
    public boolean login(String username, String password) {
        return QueryCustomer.checkPassword(username, password);
    }

    @Override
    public void run() {

        Message msg;

        while((msg = RequestManager.getNextRequest()) != null){

            switch(msg.getRequest().getTypeCase()){

                case ADDITEMTOCART:
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
