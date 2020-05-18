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
                        .setType(Request.Type.REQUEST)
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
                        .setType(Request.Type.REQUEST)
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
    public boolean addItemToCart(int userId, int itemId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setAddItemToCart(AddItemToCart.newBuilder()
                                .setUserId(userId)
                                .setItemId(itemId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return false;
    }

    @Override
    public boolean removeItemFromCart(int userId, int itemId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setRemoveItemFromCart(RemoveItemFromCart.newBuilder()
                                .setUserId(userId)
                                .setItemId(itemId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return false;
    }

    @Override
    public List<Item> getCartItems(int userId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setGetCartItems(GetCartItems.newBuilder()
                                .setUserId(userId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return null;
    }

    @Override
    public boolean order(int userId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setOrder(Order.newBuilder()
                                .setUserId(userId)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received


        return false;
    }

    @Override
    public int login(String username, String password) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setLogin(Login.newBuilder()
                                .setUsername(username)
                                .setPassword(password)
                                .build())
                        .build())
                .build();

        message = ClientDriver.request(message);

        // Do something with message received

        return message.getRequest().getLogin().getId();
    }
}

