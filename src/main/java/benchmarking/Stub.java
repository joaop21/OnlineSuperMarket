package benchmarking;

import application.Item;
import application.OnlineSuperMarket;
import client.ClientDriver;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.RequestOuterClass;
import middleware.proto.RequestOuterClass.*;

import java.util.List;
import java.util.stream.Collectors;

public class Stub implements OnlineSuperMarket {

    private Driver clientDriver = new Driver();

    @Override
    public List<Item> getItems() {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setGetItems(GetItems.newBuilder().build())
                        .build())
                .build();

        message = this.clientDriver.request(message);

        return message.getRequest().getGetItems().getItemsList().stream()
                .map(item -> new Item(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getStock()))
                .collect(Collectors.toList());

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

        message = this.clientDriver.request(message);
        RequestOuterClass.Item item = message.getRequest().getGetItem().getItem();

        return new Item(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getStock());

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

        message = this.clientDriver.request(message);

        return message.getRequest().getAddItemToCart().getAnswer();
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

        message = this.clientDriver.request(message);

        return message.getRequest().getRemoveItemFromCart().getAnswer();
    }

    @Override
    public boolean cleanCart(int userId) {

        Message message = Message.newBuilder()
                .setRequest(Request.newBuilder()
                        .setType(Request.Type.REQUEST)
                        .setCleanCart(CleanCart.newBuilder()
                                .setUserId(userId)
                                .build())
                        .build())
                .build();

        message = this.clientDriver.request(message);

        return message.getRequest().getCleanCart().getAnswer();

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

        message = this.clientDriver.request(message);

        return message.getRequest().getGetCartItems().getItemsList().stream()
                .map(item -> new Item(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getStock()))
                .collect(Collectors.toList());
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

        message = this.clientDriver.request(message);

        return message.getRequest().getOrder().getAnswer();
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

        message = this.clientDriver.request(message);

        return message.getRequest().getLogin().getId();
    }
}

