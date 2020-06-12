package database;

import application.Item;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryItem {

    private static final String DB_URL = DatabaseManager.DB_URL;

    public static Item getItem(int id){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT name, description, price, stock FROM Item WHERE id=?");
            pstat.setInt(1, id);
            ResultSet rs = pstat.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Item with that ID.");
                return null;
            }
            else {
                do {
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    float price = rs.getFloat("price");
                    int stock = rs.getInt("stock");
                    return new Item(id, name, description, price, stock);
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

    public static List<Item> getItems(){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        List<Item> res = new ArrayList<>();
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT id, name, description, price, stock FROM Item");
            ResultSet rs = pstat.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                float price = rs.getFloat("price");
                int stock = rs.getInt("stock");
                Item item = new Item(id, name, description, price, stock);
                res.add(item);
            }
            conn.close();
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }

        return res;
    }

    public static boolean updateStock(int id, int quantity){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "UPDATE Item SET stock = ? WHERE id=?");
            pstat.setInt(1, quantity);
            pstat.setInt(2, id);
            int updated = pstat.executeUpdate();
            conn.commit();
            return updated > 0;
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }

        return false;
    }
}
