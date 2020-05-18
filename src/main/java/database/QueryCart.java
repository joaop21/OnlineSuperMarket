package database;

import application.Customer;
import application.Cart;
import application.Item;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueryCart {

    private static final String DB_URL = DatabaseManager.DB_URL;

    public static Cart getCart(int userId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT begin, active FROM Customer WHERE customerid=?");
            pstat.setInt(1, userId);
            ResultSet rs = pstat.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return null;
            }
            else {
                do {
                    Timestamp begin = rs.getTimestamp("begin");
                    boolean active = rs.getBoolean("active");
                    return new Cart(userId, begin, active);
                }
                while (rs.next());
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean addItemToCart(int userId, int itemId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT active FROM Cart WHERE customerid=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return false;
            }
            else {
                do {
                    boolean active = rs.getBoolean("active");
                    if (!active) {
                        PreparedStatement ps1 = conn.prepareStatement(
                                "UPDATE Cart SET begin=NOW(), active=? WHERE customerid=" + userId);
                        ps1.setBoolean(1, true);
                        int updated = ps1.executeUpdate();
                    }
                    PreparedStatement pstat = conn.prepareStatement(
                            "INSERT IGNORE INTO Cart_Item(CartCustomerid, Itemid) VALUES(?,?,?)");
                    pstat.setInt(1, userId);
                    pstat.setInt(2, itemId);
                    int inserted = pstat.executeUpdate();
                    conn.commit();
                    return inserted > 0;
                }
                while (rs.next());
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return false;
    }

    public static boolean removeItemFromCart(int userId, int itemId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null) {
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Cart_Item WHERE CartCustomerid=? AND Itemid=?");
            ps.setInt(1, userId);
            ps.setInt(2, itemId);
            int updated = ps.executeUpdate();
            if (updated > 0){
                conn.commit();
            }
            return true;
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }

        return false;
    }

    public static List getCartItemsID(int userId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        List<Integer> result = new ArrayList<>();
        try {
            if (conn == null) {
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT Itemid FROM Cart_Item WHERE CartCustomerid=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return result;
            }
            else {
                do {
                    int itemId = rs.getInt("Itemid");
                    result.add(itemId);
                } while (rs.next());
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return result;
    }

    public static List getCartItems(int userId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        List<Item> result = new ArrayList<>();
        try {
            if (conn == null) {
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT Itemid FROM Cart_Item WHERE CartCustomerid=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return result;
            }
            else {
                do {
                    int itemId = rs.getInt("Itemid");
                    // fetch each item
                    PreparedStatement pstat = conn.prepareStatement(
                            "SELECT name, description, price, stock FROM Item WHERE id=?");
                    pstat.setInt(1, itemId);
                    ResultSet rsaux = pstat.executeQuery();
                    if (!rsaux.next()) {
                        System.out.println("There is no Item with that ID.");
                        return null;
                    }
                    else {
                        do {
                            String name = rsaux.getString("name");
                            String description = rsaux.getString("description");
                            float price = rsaux.getFloat("price");
                            int stock = rsaux.getInt("stock");
                            result.add(new Item(itemId, name, description, price, stock));
                        }
                        while (rsaux.next());
                    }
                } while (rs.next());
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return result;
    }

    public static boolean order(int userId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null) {
                System.out.println("No DB connection.");
                return false;
            }
            Savepoint spt = conn.setSavepoint();

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT Itemid FROM Cart_Item WHERE CartCustomerid=?");
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return false;
            }
            else {
                do {
                    int itemId = rs.getInt("Itemid");
                    // fetch each item
                    PreparedStatement pstat = conn.prepareStatement(
                            "SELECT name, description, price, stock FROM Item WHERE id=?");
                    pstat.setInt(1, itemId);
                    ResultSet rsaux = pstat.executeQuery();
                    if (!rsaux.next()) {
                        System.out.println("There is no Item with that ID.");
                        return false;
                    }
                    else {
                        int stock = rsaux.getInt("stock");
                        if (stock > 0) {
                            PreparedStatement update = conn.prepareStatement(
                                    "UPDATE Item SET stock=? WHERE id=?");
                            update.setInt(1, stock - 1);
                            update.setInt(2, itemId);
                            int updated = update.executeUpdate();
                        }
                        // ERROR !! The item has no stock: rollback and return false
                        else {
                            conn.rollback(spt);
                            return false;
                        }
                    }
                } while (rs.next());
            }
            // SUCCESS !!
            // Remove items from cart and set it to inactive
            PreparedStatement delete = conn.prepareStatement(
                    "DELETE FROM Cart_Item WHERE CartCustomerid=?");
            delete.setInt(1, userId);
            int deleted = delete.executeUpdate();
            if (deleted > 0) {
                PreparedStatement update = conn.prepareStatement(
                        "UPDATE Cart SET active=?, begin=? WHERE Customerid=?");
                update.setBoolean(1, false);
                update.setTimestamp(2, null);
                update.setInt(3, userId);
                int updated = update.executeUpdate();
                if (updated > 0){
                    conn.commit();
                }
                else {
                    conn.rollback(spt);
                    return false;
                }
            }
            else {
                conn.rollback(spt);
                return false;
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return true;
    }
}
