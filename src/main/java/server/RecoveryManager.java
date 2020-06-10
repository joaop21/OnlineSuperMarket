package server;

import database.DatabaseManager;
import middleware.proto.MessageOuterClass;
import middleware.proto.RecoveryOuterClass;
import middleware.server.Pair;
import middleware.spread.SpreadConnector;
import org.apache.commons.io.FileUtils;
import spread.SpreadGroup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
     *
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

    }*/

    public static boolean checkpointing() {

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

    // also does checkpoint
    public static void backup(String port) {
        Connection conn = DatabaseManager.getConnection(DatabaseManager.DB_URL);

        try {

            if (conn == null) {
                System.out.println("No DB connection.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement("BACKUP DATABASE TO " + "'backup/" + port + "/'" + " BLOCKING AS FILES");
            ps.executeUpdate();

            conn.commit();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void deleteBackup(int port, String member) {

        try {

            FileUtils.deleteDirectory(new File("databases/" + port + "/backup/" + member + "/"));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static boolean checkIfBackupExists(int port, String member) {

        Path path = Paths.get("databases/" + port + "/backup/" + member + "/");

        return Files.exists(path);

    }

    public static void compareAndSend(int port, SpreadGroup member){

        // COMPARING
        List<RecoveryOuterClass.Recovery.Line> lines = new ArrayList<>();

        try {

            Process p = Runtime.getRuntime().exec("diff databases/" + port + "/backup/" + member.toString() +
                    "/onlinesupermarket.script databases/" + port + "/onlinesupermarket.script");

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

            int index = 1;
            String s;
            while ((s = br.readLine()) != null) {

                lines.add(RecoveryOuterClass.Recovery.Line.newBuilder().setNumber(index).setData(s).build());

                index++;
            }

            p.waitFor();
            p.destroy();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        // SENDING
        MessageOuterClass.Message msg = MessageOuterClass.Message.newBuilder()
                .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                        .setType(RecoveryOuterClass.Recovery.Type.INCREMENTAL)
                        .addAllLines(lines)
                        .build())
                .build();

        SpreadConnector.send(msg.toByteArray(), member);
    }

    public static void createPatchFile(int port, MessageOuterClass.Message message) {

        try {

            Path path = Paths.get("recovery/" + port);
            Files.createDirectories(path);

            File f = new File("recovery/" + port + "/recovery.patch");
            if (!f.exists())
                f.createNewFile();

            BufferedWriter br = new BufferedWriter(new FileWriter(f));
            for(RecoveryOuterClass.Recovery.Line line : message.getRecovery().getLinesList()) {
                br.write(line.getData());
                br.newLine();
            }

            br.flush();
            br.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void patching(int port) {

        try {

            Process p = Runtime.getRuntime().exec("patch databases/" + port +
                    "/onlinesupermarket.script recovery/" + port + "/recovery.patch");

            p.waitFor();
            p.destroy();

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }

    public static void shutdown() {

        Connection conn = DatabaseManager.getConnection(DatabaseManager.DB_URL);

        try {

            if (conn == null) {
                System.out.println("No DB connection.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement("SHUTDOWN");
            ps.executeUpdate();

            conn.commit();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        List<Pair<Integer,String>> lines = getRecoveryLines(9998, 50, 60);
        for(Pair<Integer,String> line : lines)
            System.out.println(line.getFirst() + ": '" + line.getSecond() + "'");
    }

}
