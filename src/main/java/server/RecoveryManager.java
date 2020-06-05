package server;

import database.DatabaseManager;
import middleware.proto.RecoveryOuterClass;
import middleware.server.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecoveryManager {

    /**
     * Static method for checkpointing the DB and returning the DB size in lines
     *
     * @param port Port of the server for construct path string to db.
     *
     * @return Integer Number of lines of the DB.
     * */
    public static int getCurrentSize(int port) {

        // Checkpointing the DB
        if(!checkpointing())
            return 0;

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

    /**
     * Static method for retrieving the lines to recover
     *
     * @param port Port of the server for construct path string to db.
     * @param min Line number that a determined server has.
     * @param max Line number this server had when the new joined.
     *
     * @return List<Pair<Integer,String>> Lines to recover the new server
     * */
    public static List<Pair<Integer,String>> getRecoveryLines(int port, int min, int max) {

        List<Pair<Integer,String>> res = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get("databases/" + port + "/onlinesupermarket.script"), StandardCharsets.UTF_8)) {
            List<String> lines = reader.lines().skip(min).limit(max-min).collect(Collectors.toList());

            int counter = min+1;
            for(String line : lines) {
                res.add(new Pair<>(counter, line));
                counter++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;

    }

    /**
     * */
    public static void recover(int port, RecoveryOuterClass.Lines lines_proto) {

        try {

            Writer output = new BufferedWriter(new FileWriter("databases/" + port + "/onlinesupermarket.script", true));
            List<RecoveryOuterClass.Lines.Line> lines = lines_proto.getLinesList();

            int counter = lines_proto.getMin() + 1;
            for(RecoveryOuterClass.Lines.Line line : lines){

                if(line.getNumber() == counter)
                    if(line.getData().charAt(line.getData().length()-1) != '\n')
                        output.append(line.getData()).append(String.valueOf('\n'));
                    else
                        output.append(line.getData());
                else
                    System.out.println("Lines are not sorted. Be careful.");

                counter++;
            }

            output.close();

            // Checkpointing the DB
            checkpointing();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean checkpointing() {

        Connection conn = DatabaseManager.getConnection(DatabaseManager.DB_URL);

        try {

            if (conn == null) {
                System.out.println("No DB connection.");
                return false;
            }

            PreparedStatement ps = conn.prepareStatement("CHECKPOINT");
            ps.executeUpdate();

            conn.commit();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void main(String[] args) {
        List<Pair<Integer,String>> lines = getRecoveryLines(9998, 50, 60);
        for(Pair<Integer,String> line : lines)
            System.out.println(line.getFirst() + ": '" + line.getSecond() + "'");
    }

}
