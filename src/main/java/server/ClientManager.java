package server;

import application.Item;
import application.OnlineSuperMarket;
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
import java.util.ArrayList;
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
                switch (msg.getRequest().getTypeCase()){

                    case GETITEMS:
                        socketIO.write(createResponse(osm.getItems()).toByteArray());
                        break;

                    case GETITEM:
                        RequestOuterClass.GetItem getItem = msg.getRequest().getGetItem();
                        Item res2 = null;
                        switch (getItem.getTypeCase()){

                            case ITEMID:
                                res2 = osm.getItem(getItem.getItemId());
                                break;

                            case NAME:
                                res2 = osm.getItem(getItem.getName());
                                break;

                        }
                        socketIO.write(createResponse(res2).toByteArray());
                        break;

                    case ADDITEMTOCART:

                    case REMOVEITEMFROMCART:

                    case ORDER:
                        Message response_message1 = RequestManager.publishRequest(msg);
                        socketIO.write(createResponse(response_message1.getReplication().getUpdates().getStatus()).toByteArray());
                        break;

                    case GETCARTITEMS:
                        List<Item> res3 = osm.getCartItems(msg.getRequest().getGetCartItems().getUsername());
                        // send response
                        break;

                    case LOGIN:
                        RequestOuterClass.Login login = msg.getRequest().getLogin();
                        socketIO.write(createResponse(osm.login(login.getUsername(), login.getPassword())).toByteArray());
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    public Message createResponse(List<Item> items){
        List<RequestOuterClass.Item> res_items = new ArrayList<>();
        for(Item item : items){
            res_items.add(constructItem(item));
        }

        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setResponse(RequestOuterClass.Response.newBuilder()
                                .addAllItem(res_items)
                                .setStatus(true)
                                .build())
                        .build())
                .build();
    }

    public Message createResponse(Item item){
        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setResponse(RequestOuterClass.Response.newBuilder()
                                .addItem(constructItem(item))
                                .setStatus(true)
                                .build())
                        .build())
                .build();
    }

    public Message createResponse(boolean status){
        return Message.newBuilder()
                .setRequest(RequestOuterClass.Request.newBuilder()
                        .setResponse(RequestOuterClass.Response.newBuilder()
                                .setStatus(status)
                                .build())
                        .build())
                .build();
    }

    private RequestOuterClass.Item constructItem(Item item){
        return RequestOuterClass.Item.newBuilder()
                .setId(item.getId())
                .setName(item.getName())
                .setDescription(item.getDescription())
                .setPrice(item.getPrice())
                .setAvailable(item.getStock()>=1)
                .build();
    }

}
