import org.hsqldb.HsqlException;

import java.sql.*;

public class MainTest {
    public static void main(String[] args) throws SQLException {
        // if does not exist it creates a db in the specified path
        Connection c1 = DriverManager.getConnection("jdbc:hsqldb:file:databases/db1/testdb", "SA", "");
        c1.setAutoCommit(true);
        Statement statement = c1.createStatement();
        int result = statement.executeUpdate(
                "CREATE TABLE customer ( " +
                        "id INT NOT NULL, " +
                        "name VARCHAR(50) NOT NULL, " +
                        "PRIMARY KEY (id)); "
        );
        statement = c1.createStatement();
        result = statement.executeUpdate("INSERT INTO customer VALUES (1, 'User1');");
        c1.commit();
        statement = c1.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM customer;");
        while(resultSet.next()){
            System.out.println(resultSet.getInt("id")+" | "+ resultSet.getString("name"));
        }
        c1.close();

        // if does not exist it throws an exception
        try {
            Connection c2 = DriverManager.getConnection("jdbc:hsqldb:file:databases/db2/testdb;ifexists=true", "SA", "");
            c2.setAutoCommit(true);
            c2.close();
        } catch (SQLException | HsqlException e){
            System.out.println("DB does not exist.\nCreating one ...");
            Connection c2 = DriverManager.getConnection("jdbc:hsqldb:file:databases/db2/testdb;", "SA", "");
            c2.setAutoCommit(true);
            c2.close();
        }

    }
}
