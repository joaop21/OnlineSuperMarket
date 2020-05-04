package Application;

import java.util.List;

public interface OnlineSuperMarket {
    List<Item> getItems();
    Item getItem(int itemId);
    Item getItem(String itemName);
    boolean addItemToCart(String username, int itemId);
    void removeItemFromCart(String username, int itemId);
    List<Item> getCart(String username);
    boolean createOrder(String username);
    List<Order> getOrders(String username);
    boolean login(String username, String password);
}
