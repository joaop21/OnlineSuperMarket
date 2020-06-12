package application;

import java.util.List;

public interface OnlineSuperMarket {
    List<Item> getItems();
    Item getItem(int itemId);
    boolean addItemToCart(int userId, int itemId);
    boolean removeItemFromCart(int userId, int itemId);
    boolean cleanCart (int userId);
    List<Item> getCartItems(int userId);
    boolean order(int userId);
    int login(String username, String password);
}
