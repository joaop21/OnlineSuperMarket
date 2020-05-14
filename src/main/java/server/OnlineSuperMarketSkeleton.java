package server;

import application.Item;
import application.OnlineSuperMarket;
import database.QueryItem;
import middleware.gateway.Skeleton;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.RequestOuterClass;
import middleware.server.Pair;
import middleware.socket.SocketIO;
import middleware.spread.SpreadConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Set;

public class OnlineSuperMarketSkeleton extends Skeleton implements OnlineSuperMarket {
    private final SocketIO socketIO;

    public OnlineSuperMarketSkeleton(Socket sock) {
        super(sock);
        this.socketIO = new SocketIO(sock);
    }

    @Override
    public List<Item> getItems() {
        return QueryItem.getItems();
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
        return false;
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

    @Override
    public void run() {

        informLoadBalancer();

       while(true){
            try {
                Message msg = Message.parseFrom(this.socketIO.read());
                switch (msg.getRequest().getTypeCase()){

                    case GETITEMS:
                        List<Item> res1 = getItems();
                        System.out.println(res1);
                        // send response
                        break;

                    case GETITEM:
                        RequestOuterClass.GetItem getItem = msg.getRequest().getGetItem();
                        Item res2 = null;
                        switch (getItem.getTypeCase()){

                            case ITEMID:
                                res2 = getItem(getItem.getItemId());
                                break;

                            case NAME:
                                res2 = getItem(getItem.getName());
                                break;

                        }
                        // send response
                        break;

                    case ADDITEMTOCART:
                        Pair<Integer, Message> message_pair1 = Orderer.waitToProceed(msg);
                        RequestOuterClass.AddItemToCart addItemToCart = msg.getRequest().getAddItemToCart();
                        boolean status1 = false;
                        if(addItemToCart(addItemToCart.getUsername(), addItemToCart.getItemId()))
                            status1 = true;
                        // send response
                        break;

                    case REMOVEITEMFROMCART:
                        Pair<Integer, Message> message_pair2 = Orderer.waitToProceed(msg);
                        RequestOuterClass.RemoveItemFromCart removeItemFromCart = msg.getRequest().getRemoveItemFromCart();
                        removeItemFromCart(removeItemFromCart.getUsername(), removeItemFromCart.getItemId());
                        // send response
                        break;

                    case GETCARTITEMS:
                        List<Item> res3 = getCartItems(msg.getRequest().getGetCartItems().getUsername());
                        // send response
                        break;

                    case ORDER:
                        Pair<Integer, Message> message_pair3 = Orderer.waitToProceed(msg);
                        boolean status2 = order(msg.getRequest().getOrder().getUsername());
                        // send response
                        break;

                    case LOGIN:
                        RequestOuterClass.Login login = msg.getRequest().getLogin();
                        boolean status3 = login(login.getUsername(), login.getPassword());
                        // send response
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

}
