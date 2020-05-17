package database;

import application.Customer;
import application.Cart;

import java.sql.*;

public class QueryCart {

    private static final String DB_URL = DatabaseManager.DB_URL;

    public static Cart getCart(int userId){
        Connection conn = DatabaseManager.getConnection(DB_URL);
        return null;
    }
}
