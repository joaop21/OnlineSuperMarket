package middleware.loadbalancer;

import com.google.protobuf.InvalidProtocolBufferException;
import loadbalancer.Balancer;
import loadbalancer.LoadBalancerSkeleton;
import middleware.gateway.Gateway;
import middleware.proto.AssignmentOuterClass.*;
import middleware.proto.MessageOuterClass.*;
import middleware.proto.ReplicationOuterClass.*;
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

    private LoadBalancerInfo loadBalancerInfo;

    private HashMap<SpreadGroup, ServerInfo> server_info = new HashMap<>();

    public LoadBalancerMessageListener(LoadBalancerInfo loadBalancerInfo) { this.loadBalancerInfo = loadBalancerInfo; }

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
                    .collect(Collectors.toMap(c -> {return
                            ServerInfo.newBuilder()
                                    .setAddress(c.getServerInfo().getAddress())
                                    .setPort(c.getServerInfo().getPort())
                                    .build();
                    }, ServerLoads.Counter::getLoad));

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

                System.out.println(server_spread);

                ServerInfo server_socket = message.getAssignment().getServerInfo();
                System.out.println("Address: " + server_socket.getAddress() + " ; Port: " + server_socket.getPort() + " ;");

                // Storing mapping of spread group and server socket info
                server_info.put(server_spread, server_socket);

                // Adding new entry to the map that holds the load of each server
                Balancer.Balancer().add(server_socket, 0);

                // Server sending info about a client
            } else if (message.getAssignment().hasClientInfo()) {

                System.out.println("System message had client info!");

                SpreadGroup server_spread = spreadMessage.getSender();

                // Incrementing the load of this server
                Balancer.Balancer().inc(server_info.get(server_spread));

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
                                            .setAddress(((ServerInfo) e.getKey()).getAddress())
                                            .setPort(((ServerInfo) e.getKey()).getPort())
                                            .build())
                            .setLoad(e.getValue())
                            .build()).collect(Collectors.toList());

                    // Creating message with the loads replicated in it
                    Message message = Message.newBuilder()
                            .setReplication(Replication.newBuilder()
                                    .setLoads(ServerLoads.newBuilder().
                                            addAllCounter(serverLoads)
                                            .build()
                                    )
                                    .build())
                            .build();

                    // Sending loads to new load balancer
                    SpreadConnector.cast(message.toByteArray(), Set.of("LoadBalancing"));

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

    private void handleSystemInfo (MembershipInfo info) {}

    public boolean getPrimary() {return this.primary;}
}
