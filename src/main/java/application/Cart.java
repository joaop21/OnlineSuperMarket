package application;

import java.sql.Timestamp;

public class Cart {
    private int customer_id;
    private Timestamp begin;
    private boolean active;

    public Cart(int customer_id, Timestamp begin, boolean active) {
        this.customer_id = customer_id;
        this.begin = begin;
        this.active = active;
    }

    public int getCustomer_id() {
        return customer_id;
    }

    public void setCustomer_id(int customer_id) {
        this.customer_id = customer_id;
    }

    public Timestamp getBegin() {
        return begin;
    }

    public void setBegin(Timestamp begin) {
        this.begin = begin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
