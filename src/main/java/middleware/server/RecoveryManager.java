package middleware.server;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import database.DatabaseManager;
import middleware.proto.MessageOuterClass;
import middleware.proto.RecoveryOuterClass;
import middleware.spread.SpreadConnector;
import org.apache.commons.io.FileUtils;
import spread.SpreadGroup;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class RecoveryManager {

    public static MessageOuterClass.Message getRecoverInfo(int port, SpreadGroup member) {

        // Checkpointing current DB
        RecoveryManager.checkpointing();

        // Builds a message containing the difference
        return RecoveryManager.compareInitialBackup(port, member);

    }

    public static void recoverSomeone(int port, SpreadGroup member) {

        MessageOuterClass.Message msg = getRecoverInfo(port, member);

        // Send changes in DB from the start
        assert msg != null;
        SpreadConnector.unicast(msg.toByteArray(), member.toString());

    }

    public static void recoverMe(int port, MessageOuterClass.Message message) {

        // Create patch file
        RecoveryManager.createPatchFile(port, message);

        // shutdown DB and patch
        RecoveryManager.shutdown();
        RecoveryManager.patchingInitialBackup(port);

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

    public static boolean directoryExists(String path){

        return Files.exists(Paths.get(path));

    }

    private static void checkpointing() {

        Connection conn = DatabaseManager.getConnection(DatabaseManager.DB_URL);

        try {

            if (conn == null) {
                System.out.println("No DB connection.");
                return;
            }

            PreparedStatement ps = conn.prepareStatement("CHECKPOINT");
            ps.executeUpdate();

            conn.commit();

            ps.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static MessageOuterClass.Message compareInitialBackup(int port, SpreadGroup member){

        try {

            List<String> original = Files.readAllLines(new File("databases/" + port + "/backup/initial/onlinesupermarket.script").toPath());
            List<String> revised = Files.readAllLines(new File("databases/" + port + "/onlinesupermarket.script").toPath());

            return compare(original, revised);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return MessageOuterClass.Message.newBuilder()
                .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                        .setType(RecoveryOuterClass.Recovery.Type.FAILED)
                        .build())
                .build();

    }

    private static MessageOuterClass.Message compare(List<String> original, List<String> revised) {

        List<RecoveryOuterClass.Recovery.Line> lines = new ArrayList<>();

        try {

            Patch<String> diff = DiffUtils.diff(original, revised);
            List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(null, null, original, diff, 0);

            int index = 0;
            for(String line : unifiedDiff) {
                lines.add(RecoveryOuterClass.Recovery.Line.newBuilder().setNumber(index).setData(line).build());
                index++;
            }

        } catch (DiffException e) {
            e.printStackTrace();
        }

        return MessageOuterClass.Message.newBuilder()
                .setRecovery(RecoveryOuterClass.Recovery.newBuilder()
                        .setType(RecoveryOuterClass.Recovery.Type.RECOVER)
                        .addAllLines(lines)
                        .build())
                .build();
    }

    private static void createPatchFile(int port, MessageOuterClass.Message message) {

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

    private static void patchingInitialBackup(int port) {

        try {

            FileUtils.copyFile(new File("databases/" + port + "/backup/initial/onlinesupermarket.script"),
                    new File("databases/" + port + "/onlinesupermarket.script"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        patching(port);

    }

    private static void patching(int port) {

        try {

            List<String> original = Files.readAllLines(new File("databases/" + port + "/onlinesupermarket.script").toPath());
            List<String> patched = Files.readAllLines(new File("recovery/" + port + "/recovery.patch").toPath());

            Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patched);
            List<String> result = DiffUtils.patch(original, patch);

            BufferedWriter br = new BufferedWriter(new FileWriter(new File("databases/" + port + "/onlinesupermarket.script")));
            for (String line : result) {
                br.write(line);
                br.newLine();
            }
            br.flush();
            br.close();

        } catch (IOException | PatchFailedException e) {
            e.printStackTrace();
        }

    }

}
