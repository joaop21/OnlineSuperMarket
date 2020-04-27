package database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectionTest {
    public static void main(String[] args) throws SQLException {
        DatabaseManager.createDatabase("jdbc:hsqldb:file:databases/db1/serverdb;");
        Connection conn = DatabaseManager.getConnection("jdbc:hsqldb:file:databases/db1/serverdb;");
        assert conn != null;
        Statement s = conn.createStatement();
        ResultSet resultSet = s.executeQuery("SELECT * FROM item;");
        while(resultSet.next()){
            System.out.println(resultSet.getInt("id")+" | "+ resultSet.getString("name"));
        }
        conn.close();
    }
}
