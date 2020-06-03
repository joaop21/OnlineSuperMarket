package server;

import database.DatabaseManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class RecoveryManager {

    public static int getCurrentSize(int port) {

        // Checkpointing the DB
        Connection conn = DatabaseManager.getConnection(DatabaseManager.DB_URL);

        try {

            if (conn == null) {
                System.out.println("No DB connection.");
                return 0;
            }

            PreparedStatement ps = conn.prepareStatement("CHECKPOINT");
            ps.executeUpdate();

            conn.commit();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Counting size of file based on its lines
        int lines = 0;

        try {

            BufferedReader reader = new BufferedReader(new FileReader("databases/" + port + "/onlinesupermarket.script"));
            while (reader.readLine() != null) lines++;
            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return lines;

    }

}
