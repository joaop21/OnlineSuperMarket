package server;

import application.Item;
import application.OnlineSuperMarket;
import middleware.gateway.Skeleton;

import java.net.Socket;
import java.util.List;

public class OnlineSuperMarketSkeleton extends Skeleton implements OnlineSuperMarket {
    public OnlineSuperMarketSkeleton(Socket sock) {
        super(sock);
    }

    @Override
    public List<Item> getItems() {
        return null;
    }

    @Override
    public Item getItem(int itemId) {
        return null;
    }

    @Override
    public Item getItem(String itemName) {
        return null;
    }

    @Override
    public boolean addItemToCart(String username, int itemId) {
        return false;
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
        return false;
    }

    @Override
    public void run() {

    }
}
