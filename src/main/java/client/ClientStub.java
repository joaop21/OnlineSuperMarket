package client;

import application.Item;
import application.OnlineSuperMarket;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.RequestOuterClass.*;

import java.util.List;

public class ClientStub implements OnlineSuperMarket {

    @Override
    public List<Item> getItems() {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setGetItems(GetItems.newBuilder().build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return null;
    }

    @Override
    public Item getItem(int itemId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setGetItem(GetItem.newBuilder()
                                .setItemId(itemId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return null;

    }

    @Override
    public Item getItem(String itemName) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setGetItem(GetItem.newBuilder()
                                .setName(itemName)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return null;
    }

    @Override
    public boolean addItemToCart(String username, int itemId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setAddItemToCart(AddItemToCart.newBuilder()
                                .setUsername(username)
                                .setItemId(itemId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return false;
    }

    @Override
    public void removeItemFromCart(String username, int itemId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setRemoveItemFromCart(RemoveItemFromCart.newBuilder()
                                .setUsername(username)
                                .setItemId(itemId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

    }

    @Override
    public List<Item> getCartItems(String username) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setGetCartItems(GetCartItems.newBuilder()
                                .setUsername(username)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return null;
    }

    @Override
    public boolean order(String username) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setOrder(Order.newBuilder()
                                .setUsername(username)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received


        return false;
    }

    @Override
    public boolean login(String username, String password) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setLogin(Login.newBuilder()
                                .setUsername(username)
                                .setPassword(password)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return false;
    }
}

