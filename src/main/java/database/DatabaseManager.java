package database;

import org.hsqldb.HsqlException;

import java.sql.*;
import java.util.List;

public class DatabaseManager {

    // FOR TESTING PURPOSES: USE IN EACH SERVER TH DB DEDICATED TO IT
    public static String DB_URL = "jdbc:hsqldb:file:databases/db/onlinesupermarket";

    public static void createDatabase(String url){
        DB_URL = url;
        try {
            Connection conn = DriverManager.getConnection(url+";ifexists=true", "SA", "");
            conn.close();
            System.out.println("DB already exists. You cannot create a new one with the same name.");
        } catch (SQLException | HsqlException e1){
            try {
                Connection conn = DriverManager.getConnection(url, "SA", "");

                PreparedStatement pstat = conn.prepareStatement("SET DATABASE SQL SYNTAX MYS TRUE;");
                pstat.executeUpdate();

                pstat = conn.prepareStatement(
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
            Connection conn = DriverManager.getConnection(url, "SA", "");
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void initialLoad(Connection conn) {
        try {
            String customer = "INSERT INTO Customer(username, password) VALUES(?,?)";
            String item = "INSERT INTO Item(name, description, price, stock) VALUES(?,?,?,?)";
            String cart = "INSERT INTO Cart(customerid, begin, active) VALUES(?,?,?)";

            // Populate Customers
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

            // Populate items
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
            ps5.setInt(4, 2);
            ps5.executeUpdate();

            // Populate Customers' Carts
            PreparedStatement ps6 = conn.prepareStatement(cart);
            ps6.setInt(1, 0);
            ps6.setTimestamp(2, null);
            ps6.setBoolean(3, false);
            ps6.executeUpdate();
            PreparedStatement ps7 = conn.prepareStatement(cart);
            ps7.setInt(1, 1);
            ps7.setTimestamp(2, null);
            ps7.setBoolean(3, false);
            ps7.executeUpdate();
            PreparedStatement ps8 = conn.prepareStatement(cart);
            ps8.setInt(1, 2);
            ps8.setTimestamp(2, null);
            ps8.setBoolean(3, false);
            ps8.executeUpdate();

            System.out.println("Loaded successfully.");
        }
        catch(SQLException e){
            System.out.println("Something went wrong while populating the DB.");
            e.printStackTrace();
        }
    }

    private static Object getValue(Object value) {
        if (value == null){
            return "NULL";
        }
        if (value instanceof Integer || value instanceof Boolean) {
            return value;
        }
        else if (value instanceof Timestamp || value instanceof String){
            return "\'" + value + "\'";
        }
        else return value;
    }

    private static String buildSQLInsert(String table, List<FieldValue> vals){
        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(table).append("(");
        for(int i=0; i<vals.size(); i++){
            query.append(vals.get(i).getField());
            if (i != vals.size() - 1){
                query.append(",");
            }
            else{
                query.append(") VALUES(");
            }
        }
        for(int i=0; i<vals.size(); i++){
            query.append(getValue(vals.get(i).getValue()));
            if (i != vals.size() - 1){
                query.append(",");
            }
            else{
                query.append(")");
            }
        }
        return query.toString();
    }

    private static String buildSQLUpdate(String table, List<FieldValue> vals, List<FieldValue> where){
        StringBuilder query = new StringBuilder("UPDATE ");
        query.append(table).append(" SET ");
        for(int i=0; i<vals.size(); i++){
            FieldValue fv = vals.get(i);
            query.append(fv.getField()).append("=");
            query.append(getValue(fv.getValue()));
            if (i != vals.size() - 1){
                query.append(", ");
            }
            else{
                query.append(" WHERE ");
            }
        }
        for(int i=0; i<where.size(); i++){
            FieldValue fv = where.get(i);
            query.append(fv.getField()).append("=");
            query.append(getValue(fv.getValue()));
            if (i != where.size() - 1){
                query.append(" AND ");
            }
        }
        return query.toString();
    }

    private static String buildSQLDelete(String table, List<FieldValue> where){
        StringBuilder query = new StringBuilder("DELETE FROM ");
        query.append(table).append(" WHERE ");
        for(int i=0; i<where.size(); i++){
            FieldValue fv = where.get(i);
            query.append(fv.getField()).append("=");
            query.append(getValue(fv.getValue()));
            if (i != where.size() - 1){
                query.append(" AND ");
            }
        }
        return query.toString();
    }

    public static void loadModifications(List<DatabaseModification> mods){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        if (conn == null){
            System.out.println("No connection.");
            return;
        }
        for(DatabaseModification mod : mods){
            switch(mod.getType()){
                // INSERT
                case 0:
                    System.out.println("INSERT");
                    String insertQuery = buildSQLInsert(mod.getTable(), mod.getMods());
                    System.out.println(insertQuery);
                    try{
                        PreparedStatement ps = conn.prepareStatement(insertQuery);
                        int inserted = ps.executeUpdate();
                        if (inserted > 0)
                            System.out.println("Inserted new row in "+mod.getTable());
                        ps.close();
                    }
                    catch(SQLException e){
                        System.out.println("An error occurred while executing the SQL query.");
                        e.printStackTrace();
                        return;
                    }
                    break;
                // UPDATE
                case 1:
                    System.out.println("UPDATE");
                    String updateQuery = buildSQLUpdate(mod.getTable(), mod.getMods(), mod.getWhere());
                    System.out.println(updateQuery);
                    try{
                        PreparedStatement ps = conn.prepareStatement(updateQuery);
                        int updated = ps.executeUpdate();
                        if (updated > 0)
                            System.out.println("Updated "+updated+" rows in "+mod.getTable());
                        ps.close();
                    }
                    catch(SQLException e){
                        System.out.println("An error occurred while executing the SQL query.");
                        e.printStackTrace();
                        return;
                    }
                    break;
                // DELETE
                case 2:
                    System.out.println("DELETE");
                    String deleteQuery = buildSQLDelete(mod.getTable(), mod.getWhere());
                    System.out.println(deleteQuery);
                    try{
                        PreparedStatement ps = conn.prepareStatement(deleteQuery);
                        int deleted = ps.executeUpdate();
                        if (deleted > 0)
                            System.out.println("Deleted "+deleted+" rows in "+mod.getTable());
                        ps.close();
                    }
                    catch(SQLException e){
                        System.out.println("An error occurred while executing the SQL query.");
                        e.printStackTrace();
                        return;
                    }
                    break;
                default:
                    System.out.println("Operation not supported");
                    return;
            }
        }

        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

    }
}
