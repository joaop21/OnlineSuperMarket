package middleware.loadbalancer;

import com.google.protobuf.InvalidProtocolBufferException;
import loadbalancer.Balancer;
import loadbalancer.LoadBalancerSkeleton;
import middleware.gateway.Gateway;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.ReplicationOuterClass.*;
import middleware.socket.SocketInfo;
import middleware.spread.SpreadConnector;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;
import java.util.stream.Collectors;

public class LoadBalancerMessageListener implements AdvancedMessageListener  {

    private List<String> leader_fifo = new LinkedList<>();
    private String myself;

    private boolean first_message = true;
    private boolean primary = false;
    private boolean balancer_set = false;

    private SocketInfo loadBalancerInfo;

    private HashMap<SpreadGroup, SocketInfo> server_info = new HashMap<>();

    public LoadBalancerMessageListener(SocketInfo loadBalancerInfo) { this.loadBalancerInfo = loadBalancerInfo; }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {

        try {

            Message message = Message.parseFrom(spreadMessage.getData());

            if (message.hasAssignment()) handleAssginmentMessage(spreadMessage);
            else if (message.hasReplication()) handleReplicationMessage(spreadMessage);

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

        }

    }

    private void handleReplicationMessage(SpreadMessage spreadMessage) {

        try {

            System.out.println("Received load balancing message for replication!");

            // If we've set the balancer already, no point in doing it again
            if (balancer_set) return;

            // Getting loads from the primary Load Balancer
            ServerLoads serverLoads = Message.parseFrom(spreadMessage.getData()).getReplication().getLoads();

            // Unmarshalling the counters as a Map known by the balancer
            Map<Object, Integer> counters = serverLoads.getCounterList().stream()
                    .collect(Collectors.toMap(c -> new SocketInfo( c.getServerInfo().getAddress(),
                                                                   c.getServerInfo().getPort())
                            , ServerLoads.Counter::getLoad));

            // Setting the counters to the balancer
            Balancer.Balancer().set(counters);

            // Setting balancer as set
            this.balancer_set = true;

        } catch (InvalidProtocolBufferException e) {

            e.printStackTrace();

        }

    }

    private void handleAssginmentMessage(SpreadMessage spreadMessage) {

        try {

            System.out.println("Received system message!");

            Message message = Message.parseFrom(spreadMessage.getData());

            // Server sending own info for the first time
            if (message.getAssignment().hasServerInfo() && !server_info.containsKey(spreadMessage.getSender())) {

                System.out.println("System message had server info!");

                SpreadGroup server_spread = spreadMessage.getSender();

                System.out.println("Server spread: " + server_spread);

                ServerInfo server_socket = message.getAssignment().getServerInfo();
                System.out.println("Address: " + server_socket.getAddress() + " ; Port: " + server_socket.getPort() + " ;");

                // Storing mapping of spread group and server socket info
                server_info.put(server_spread, new SocketInfo(server_socket.getAddress(), server_socket.getPort()));

                // Adding new entry to the map that holds the load of each server
                Balancer.Balancer().add(new SocketInfo(server_socket.getAddress(), server_socket.getPort()), 0);

                // Server sending info about a client
            } else if (message.getAssignment().hasClientInfo()) {

                System.out.println("System message had client info!");

                SpreadGroup server_spread = spreadMessage.getSender();

                switch (message.getAssignment().getClientInfo().getState()) {

                    case CONNECTED:
                        Balancer.Balancer().inc(server_info.get(server_spread));
                        break;
                    case DISCONNECTED:
                        Balancer.Balancer().dec(server_info.get(server_spread));
                        break;

                }

                System.out.println("Socket Info to change: " + server_info.get(server_spread).getAddress() + " - " + server_info.get(server_spread).getPort() );

            }

    } catch (InvalidProtocolBufferException e) {

        e.printStackTrace();

    }

    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {

        MembershipInfo info = spreadMessage.getMembershipInfo();

        switch (info.getGroup().toString()) {

            case "LoadBalancing":
                handleLoadBalancingInfo(info);
                break;

            case "System":
                handleSystemInfo(info);
                break;

        }
    }

    private void handleLoadBalancingInfo (MembershipInfo info) {

        System.out.println("Received Load Balancing Info!");

        if (info.isCausedByJoin()) { // Someone joined the arena

            System.out.println("Info was caused by join!");

            if (this.first_message) { // I joined

                System.out.println("I joined!");

                this.first_message = false;
                this.myself = info.getJoined().toString();

                for (SpreadGroup g : info.getMembers())
                    if (!g.toString().equals(myself))
                        this.leader_fifo.add(g.toString());

                
                System.out.println("Casting Load Balancer Info!");
                // Sending own info
                Message message = Message.newBuilder()
                        .setAssignment(Assignment.newBuilder()
                                .setLoadBalancerInfo(
                                        LoadBalancerInfo.newBuilder()
                                                .setAddress(loadBalancerInfo.getAddress())
                                                .setPort(loadBalancerInfo.getPort())
                                                .build())
                                .build())
                        .build();

                SpreadConnector.cast(message.toByteArray(), Set.of("System"));

            } else { // Someone else joined

                System.out.println("Someone else joined!");

                if (this.primary) {

                    System.out.println("Sending counters to the load balancer that joined!");

                    // Getting current server loads from the balancer
                    Map<Object, Integer> counters = Balancer.Balancer().get();

                    // Collecting the loads to a list of counters that can be marshalled
                    List<ServerLoads.Counter> serverLoads= counters.entrySet().stream().map(e ->
                            ServerLoads.Counter.newBuilder()
                            .setServerInfo(
                                    ServerLoads.Counter.ServerInfo.newBuilder()
                                            .setAddress(((SocketInfo) e.getKey()).getAddress())
                                            .setPort(((SocketInfo) e.getKey()).getPort())
                                            .build())
                            .setLoad(e.getValue())
                            .build()).collect(Collectors.toList());

                    // Creating message with the loads replicated in it
                    Message message = Message.newBuilder()
                            .setReplication(Replication.newBuilder()
                                    .setLoads(ServerLoads.newBuilder().
                                            addAllCounter(serverLoads)
                                            .build())
                                    .build())
                            .build();

                    // Sending loads to new load balancer
                    SpreadConnector.send(message.toByteArray(), info.getJoined());

                }

            }

            leader_fifo.add(info.getJoined().toString());

        } else if (info.isCausedByDisconnect()) // Someone disconnected from the arena

            this.leader_fifo.removeIf(member -> member.equals(info.getDisconnected().toString()));

        else if (info.isCausedByLeave()) // Someone left the arena

            this.leader_fifo.removeIf(member -> member.equals(info.getLeft().toString()));

        // Becoming master of the arena
        if (this.leader_fifo.get(0).equals(this.myself) && !this.primary) {

            // Setting myself as primary
            this.primary = true;

            // Setting balancer as set
            this.balancer_set = true;

            // Starting acceptor of client connections
            new Gateway(loadBalancerInfo.getPort(), LoadBalancerSkeleton.class);

        }

        if (this.primary) System.out.println("I'm primary!");
        else System.out.println("I'm useless!");

    }

    private void handleSystemInfo (MembershipInfo info) {

        System.out.println("Received System Info!");

        if (info.isCausedByJoin()) {

            System.out.println("Sending Load Balancer Info!");

            // Sending own info
            Message message = Message.newBuilder()
                    .setAssignment(Assignment.newBuilder()
                            .setLoadBalancerInfo(
                                    LoadBalancerInfo.newBuilder()
                                            .setAddress(loadBalancerInfo.getAddress())
                                            .setPort(loadBalancerInfo.getPort())
                                            .build())
                            .build())
                    .build();

            SpreadConnector.send(message.toByteArray(), info.getJoined());


        } else if (info.isCausedByDisconnect()) // Someone disconnected from the arena

            if (server_info.containsKey(info.getDisconnected())) {

                // Removing from Balancer
                Balancer.Balancer().rem(server_info.get(info.getDisconnected()));

                // Removing from known server spread groups
                server_info.remove(info.getDisconnected());

            }

        else if (info.isCausedByLeave()) // Someone left the arena

                if (server_info.containsKey(info.getLeft())) {

                    // Removing from Balancer
                    Balancer.Balancer().rem(server_info.get(info.getLeft()));

                    // Removing from known server spread groups
                    server_info.remove(info.getLeft());

                }

    }

    public boolean getPrimary() {return this.primary;}
}
