package application;

import java.util.List;

public interface OnlineSuperMarket {
    List<Item> getItems();
    Item getItem(int itemId);
    Item getItem(String itemName);
    boolean addItemToCart(int userId, int itemId);
    void removeItemFromCart(int userId, int itemId);
    List<Item> getCartItems(int userId);
    boolean order(int userId);
    int login(String username, String password);
}
