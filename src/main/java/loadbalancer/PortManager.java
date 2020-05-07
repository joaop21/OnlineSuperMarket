package loadbalancer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PortManager {

    // Static variable single_instance that governs instances
    private static PortManager port_manager = null;

    private ConcurrentHashMap<Integer, Integer> server_ports;

    private PortManager() {

        this.server_ports = new ConcurrentHashMap<>(Map.of(10001,0, 10002, 0, 10003, 0));

    }

    public static PortManager PortManager() {

        // Ensuring only one instance is created
        if (port_manager == null) {

            System.out.println("CREATING PORT MANAGER");
            port_manager = new PortManager();

        }
        return port_manager;

    }

    public int getServerPort () {

        Map.Entry<Integer, Integer> chosen_entry = null;
        for (Map.Entry<Integer, Integer> entry: server_ports.entrySet())
            if (chosen_entry == null || entry.getValue() < chosen_entry.getValue())
                chosen_entry = entry;

        server_ports.put(chosen_entry.getKey(), chosen_entry.getValue() + 1);

        return chosen_entry.getKey();

    }

    public boolean addServerPort (int port) { return server_ports.putIfAbsent(port, 0) == null; }

    public boolean removeServerPort (int port) { return server_ports.remove(port) != null; }

}
