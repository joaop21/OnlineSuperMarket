package database;

import org.hsqldb.HsqlException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    public static void createDatabase(String url){
        try {
            Connection conn = DriverManager.getConnection(url+"ifexists=true", "SA", "");
            conn.close();
            System.out.println("DB already exists. You cannot create a new one with the same name.");
        } catch (SQLException | HsqlException e1){
            try {
                Connection conn = DriverManager.getConnection(url, "SA", "");
                PreparedStatement pstat = conn.prepareStatement(
                        "CREATE TABLE item (" +
                                "id INT NOT NULL, " +
                                "name VARCHAR(255) NOT NULL, " +
                                "description VARCHAR(255), " +
                                "price FLOAT NOT NULL, " +
                                "stock INT NOT NULL, " +
                                "PRIMARY KEY (id));");
                pstat.executeUpdate();
                conn.close();
            } catch (SQLException e2) {
                System.out.println("Cannot create DB due to an error.");
                e2.printStackTrace();
            }
        }
    }

    public static Connection getConnection(String url){
        try {
            return DriverManager.getConnection(url, "SA", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
