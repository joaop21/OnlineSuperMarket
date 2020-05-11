package database;

import application.Customer;
import application.Item;

import database.DatabaseManager;

import java.sql.*;

public class Query {

    private static final String DB_URL = DatabaseManager.DB_URL;

    // CUSTOMER LOGIC
    public Customer getCustomer(int id){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT username, password FROM Customer WHERE id=" + id);
            ResultSet rs = pstat.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that ID.");
                return null;
            }
            else {
                do {
                    String username = rs.getString("username");
                    String password = rs.getString("password");
                    return new Customer(id, username, password);
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

    public boolean check_password(String username, String password){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT username, password FROM Customer WHERE username=?");
            pstat.setString(1,username);
            ResultSet rs = pstat.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that username.");
                return false;
            }
            else {
                do {
                    String customerPassword = rs.getString("password");
                    return customerPassword.equals(password);
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

    public int new_customer(String username, String password){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return -1;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "INSERT INTO Customer(username, password) "+
                            "VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
            pstat.setString(1, username);
            pstat.setString(2, password);
            int inserted = pstat.executeUpdate();
            if (inserted > 0){
                ResultSet rs = pstat.getGeneratedKeys();
                if(rs.next())
                {
                    return rs.getInt(1);
                }
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return -1;
    }
}
