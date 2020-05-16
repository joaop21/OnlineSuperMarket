package application;

public class Item {
    private int id;
    private String name;
    private String description;
    private float price;
    private int stock;

    public Item(int id, String name, String description, float price, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String toString(){
        return "<ITEM id="+ this.id +
                ", name=" + this.name +
                ", price=" + this.price +
                ", stock=" + this.stock +
                ", description=" + this.description
                +">";
    }

    public String toPrettyString() {

        return "# " + this.id +
                " -> " + this.name +
                " ( " + this.price + " €)" +
                " - " + ((this.stock > 0) ? this.stock + " in Stock"  : "Out of Stock");

    }

    public String toDescriptionString () {

        return "# Id: " + this.id + "\n" +
                "# Name: " + this.name + "\n" +
                "# Description: " + this.description + " \n" +
                "# Price: " + this.price + " €\n" +
                "# Stock: " + this.stock;

    }
}
