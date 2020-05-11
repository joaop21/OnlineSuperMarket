package database;

import org.hsqldb.HsqlException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    // FOR TESTING PURPOSES: USE IN EACH SERVER TH DB DEDICATED TO IT
    public static final String DB_URL = "jdbc:hsqldb:file:databases/db/onlinesupermarket";

    public static void createDatabase(String url){
        try {
            Connection conn = DriverManager.getConnection(url+";ifexists=true", "SA", "");
            conn.close();
            System.out.println("DB already exists. You cannot create a new one with the same name.");
        } catch (SQLException | HsqlException e1){
            try {
                Connection conn = DriverManager.getConnection(url, "SA", "");
                PreparedStatement pstat = conn.prepareStatement(
                        "CREATE TABLE Customer ( "+
                            "id integer IDENTITY PRIMARY KEY, "+
                            "username varchar(255) NOT NULL UNIQUE, "+
                            "password varchar(255) NOT NULL"+
                            ");");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "CREATE TABLE Item ( "+
                            "id integer IDENTITY PRIMARY KEY, "+
                            "name varchar(255) NOT NULL, "+
                            "description varchar(255), "+
                            "price float NOT NULL, "+
                            "stock integer NOT NULL"+
                            ");");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "CREATE TABLE Cart ("+
                            "Customerid integer IDENTITY PRIMARY KEY, "+
                            "begin timestamp, "+
                            "active boolean NOT NULL"+
                            ");");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "CREATE TABLE Cart_Item ("+
                            "CartCustomerid integer NOT NULL, "+
                            "Itemid integer NOT NULL, "+
                            "PRIMARY KEY (CartCustomerid, Itemid));");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "ALTER TABLE Cart ADD CONSTRAINT FKCart197871 FOREIGN KEY (Customerid) REFERENCES Customer (id);");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "ALTER TABLE Cart_Item ADD CONSTRAINT FKCart_Item2178 FOREIGN KEY (CartCustomerid) REFERENCES Cart (Customerid);");
                pstat.executeUpdate();
                pstat = conn.prepareStatement(
                        "ALTER TABLE Cart_Item ADD CONSTRAINT FKCart_Item222313 FOREIGN KEY (Itemid) REFERENCES Item (id);");
                pstat.executeUpdate();

                conn.commit();
                conn.close();

                System.out.println("Database created with success.");
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
