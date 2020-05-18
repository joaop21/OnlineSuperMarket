package server;

import application.Item;
import application.OnlineSuperMarket;
import middleware.gateway.Skeleton;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.RequestOuterClass;
import middleware.socket.SocketIO;
import middleware.spread.SpreadConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class ClientManager extends Skeleton {
    private final SocketIO socketIO;

    public ClientManager(Socket sock) {
        super(sock);
        this.socketIO = new SocketIO(sock);
    }

    @Override
    public void run() {

        informLoadBalancer();

        OnlineSuperMarket osm = new OnlineSuperMarketSkeleton();

        while(true){
            try {
                Message msg = Message.parseFrom(this.socketIO.read());
                switch (msg.getRequest().getOperationCase()){

                    case GETITEMS:
                        socketIO.write(
                                getGetItemsResponse(msg, osm.getItems()).toByteArray());
                        break;
                    case GETITEM:
                        socketIO.write(
                                getGetItemResponse(msg, osm.getItem(msg.getRequest().getGetItem().getItemId())).toByteArray());
                        break;
                    case ADDITEMTOCART:
                        socketIO.write(
                                getAddItemToCartResponse(msg, osm.addItemToCart(msg.getRequest().getAddItemToCart().getUserId(),
                                        msg.getRequest().getAddItemToCart().getItemId())).toByteArray());
                        break;
                    case REMOVEITEMFROMCART:
                        socketIO.write(
                                getRemItemFromCart(msg, osm.removeItemFromCart(msg.getRequest().getRemoveItemFromCart().getUserId(),
                                    msg.getRequest().getRemoveItemFromCart().getItemId())).toByteArray());
                        break;
                    case ORDER:
                        socketIO.write(
                                getOrderResponse(msg, osm.order(msg.getRequest().getOrder().getUserId())).toByteArray());
                        break;

                    case GETCARTITEMS:
                        socketIO.write(
                                getGetCartItemsResponse(msg, osm.getCartItems(msg.getRequest().getGetCartItems().getUserId())).toByteArray());
                        break;

                    case LOGIN:
                        socketIO.write(
                                getLoginResponse(msg, osm.login(msg.getRequest().getLogin().getUsername(),
                                        msg.getRequest().getLogin().getPassword())).toByteArray());
                        break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Message getGetItemsResponse(Message msg, List<Item> items) {

        RequestOuterClass.GetItems.Builder getItems = RequestOuterClass.GetItems.newBuilder();

        for (Item item: items)
            getItems.addItems(
                    RequestOuterClass.Item.newBuilder()
                            .setId(item.getId())
                            .setName(item.getName())
                            .setDescription(item.getDescription())
                            .setPrice(item.getPrice())
                            .setStock(item.getStock())
                            .build());

        return Message.newBuilder()
                    .setRequest(RequestOuterClass.Request.newBuilder()
                            .setType(RequestOuterClass.Request.Type.REPLY)
                            .setGetItems(getItems.build())
                            .build())
                    .build();

    }

    private Message getGetItemResponse(Message msg, Item item) {

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setGetItem(RequestOuterClass.GetItem.newBuilder()
                                .setItem(RequestOuterClass.Item.newBuilder()
                                        .setId(item.getId())
                                        .setName(item.getName())
                                        .setDescription(item.getDescription())
                                        .setPrice(item.getPrice())
                                        .setStock(item.getStock())
                                        .build())
                                .build())
                        .build())
                .build();

    }

    private Message getAddItemToCartResponse(Message msg, boolean addItemToCart) {

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setAddItemToCart(RequestOuterClass.AddItemToCart.newBuilder()
                                .setAnswer(addItemToCart)
                                .build())
                        .build())
                .build();

    }

    private Message getRemItemFromCart(Message msg, boolean remItemFromCart) {

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setRemoveItemFromCart(RequestOuterClass.RemoveItemFromCart.newBuilder()
                                .setAnswer(remItemFromCart)
                                .build())
                        .build())
                .build();

    }

    private Message getOrderResponse(Message msg, boolean ordered) {

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setOrder(RequestOuterClass.Order.newBuilder()
                                .setAnswer(ordered)
                                .build())
                        .build())
                .build();

    }

    private Message getGetCartItemsResponse(Message msg, List<Item> cartItems) {

        RequestOuterClass.GetCartItems.Builder getCartItems = RequestOuterClass.GetCartItems.newBuilder();

        for (Item item: cartItems)
            getCartItems.addItems(
                    RequestOuterClass.Item.newBuilder()
                            .setId(item.getId())
                            .setName(item.getName())
                            .setDescription(item.getDescription())
                            .setPrice(item.getPrice())
                            .setStock(item.getStock())
                            .build());

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setGetCartItems(getCartItems.build())
                        .build())
                .build();

    }

    private Message getLoginResponse(Message msg, int login) {

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setType(RequestOuterClass.Request.Type.REPLY)
                        .setLogin(RequestOuterClass.Login.newBuilder()
                                .setAnswer(login >= 0)
                                .setId(login)
                                .build())
                        .build())
                .build();

    }

    public void informLoadBalancer(){

        // Creating client-server info message
        Message message = Message.newBuilder()
                .setAssignment(Assignment.newBuilder()
                        .setClientInfo(ClientInfo.newBuilder()
                                .setAddress(((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress())
                                .setPort(((InetSocketAddress) socket.getRemoteSocketAddress()).getPort())
                                .setState(ClientInfo.State.CONNECTED)
                                .build())
                        .build())
                .build();

        System.out.println("Sending message to other Load Balancers!");

        SpreadConnector.cast(message.toByteArray(), Set.of("System"));

    }


}
