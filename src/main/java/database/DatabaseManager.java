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

                System.out.println("Database created with success.");

                // POPULATE
                initialLoad(conn);

                conn.commit();
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

    private static void initialLoad(Connection conn) {
        try {
            String customer = "INSERT INTO Customer(username, password) VALUES(?,?)";
            String item = "INSERT INTO Item(name, description, price, stock) VALUES(?,?,?,?)";

            PreparedStatement ps1 = conn.prepareStatement(customer);
            ps1.setString(1, "henrique");
            ps1.setString(2, "henrique");
            ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement(customer);
            ps2.setString(1, "joao");
            ps2.setString(2, "joao");
            ps2.executeUpdate();
            PreparedStatement ps3 = conn.prepareStatement(customer);
            ps3.setString(1, "miguel");
            ps3.setString(2, "miguel");
            ps3.executeUpdate();

            PreparedStatement ps4 = conn.prepareStatement(item);
            ps4.setString(1, "Máscara");
            ps4.setString(2, "Máscara contra o COVID-19");
            ps4.setFloat(3, 50);
            ps4.setInt(4, 5);
            ps4.executeUpdate();
            PreparedStatement ps5 = conn.prepareStatement(item);
            ps5.setString(1, "Papel Higiénico");
            ps5.setString(2, "Papel Higiénico contra o COVID-19");
            ps5.setFloat(3, 100);
            ps5.setInt(4, 10);
            ps5.executeUpdate();

            System.out.println("Loaded successfully.");
        }
        catch(SQLException e){
            System.out.println("Something went wrong while populating the DB.");
            e.printStackTrace();
        }
    }
}
