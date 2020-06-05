package database;

import application.Customer;

import java.sql.*;

public class QueryCustomer {

    private static final String DB_URL = DatabaseManager.DB_URL;

    // CUSTOMER LOGIC
    public static Customer getCustomer(int id){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return null;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT username, password FROM Customer WHERE id=?");
            pstat.setInt(1, id);
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

    // returns user ID if the username and password match or -1 if not
    public static int checkPassword(String username, String password){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return -1;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "SELECT * FROM Customer WHERE username=?");
            pstat.setString(1, username);
            ResultSet rs = pstat.executeQuery();
            if (!rs.next()) {
                System.out.println("There is no Customer with that username.");
                conn.close();
                return -2;
            }
            else {
                do {
                    String customerPassword = rs.getString("password");
                    if (customerPassword.equals(password)) {
                        conn.close();
                        return rs.getInt("id");
                    }
                    else {
                        conn.close();
                        return -3;
                    }
                }
                while (rs.next());
            }
        }
        catch(SQLException e) {
            System.out.println("An error occurred while executing the SQL query.");
            e.printStackTrace();
        }
        return -4;
    }

    // returns the new user ID
    public static int newCustomer(String username, String password){
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
            conn.commit();
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

    public static boolean updateCustomerUsername(int id, String username){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "UPDATE Customer "+
                            "SET username=? "+
                            "WHERE id=?");
            pstat.setString(1, username);
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

    public static boolean updateCustomerPassword(int id, String password){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        try {
            if (conn == null){
                System.out.println("No DB connection.");
                return false;
            }
            PreparedStatement pstat = conn.prepareStatement(
                    "UPDATE Customer "+
                            "SET password=? "+
                            "WHERE id=?");
            pstat.setString(1, password);
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
