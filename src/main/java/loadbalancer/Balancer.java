package loadbalancer;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Balancer {

    // Static variable single_instance that governs instances
    private static Balancer balancer = null;

    private ConcurrentHashMap<Object, Integer> counters;

    private Balancer() {

        this.counters = new ConcurrentHashMap<>();

    }

    public static Balancer Balancer() {

        // Ensuring only one instance is created
        if (balancer == null) {

            balancer = new Balancer();

        }
        return balancer;

    }

    public boolean add (Object key, Integer value) { return counters.putIfAbsent(key, value) == null; }

    public boolean rem (Object key) { return counters.remove(key) != null; }

    public boolean inc (Object key) {

        if (!counters.containsKey(key)) return false;

        // Incrementing
        counters.put(key, counters.get(key) + 1);

        return true;

    }

    public Object min () {

        Map.Entry<Object, Integer> chosen_entry = null;
        for (Map.Entry<Object, Integer> entry: counters.entrySet())
            if (chosen_entry == null || entry.getValue() < chosen_entry.getValue())
                chosen_entry = entry;

        return (chosen_entry != null) ? chosen_entry.getKey() : null;

    }

    public Map<Object, Integer> get () { return new ConcurrentHashMap<>(counters); }

    public void set (Map<Object, Integer> counters) { this.counters = new ConcurrentHashMap<>(counters); }
}
