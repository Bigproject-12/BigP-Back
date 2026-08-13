import java.sql.*;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileService {

    private static final String JWT_SECRET = System.getenv("JWT_SECRET");
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {

        if (JWT_SECRET == null || OPENAI_API_KEY == null || URL == null || USER == null || PASSWORD == null) {
            throw new IllegalArgumentException("Missing required environment variables: JWT_SECRET, OPENAI_API_KEY, DB_URL, DB_USER, DB_PASSWORD");
        }

        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);

        runCommand("dir");

        System.out.println(sha256(password));
        // System.out.println("JWT Secret : " + JWT_SECRET); // Removed sensitive log
        // System.out.println("API KEY : " + OPENAI_API_KEY); // Removed sensitive log
    }

    public static void saveUser(String username, String password) {

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users(username,password) VALUES(?,?)")) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();

            for (int i = 0; i < 10000; i++) {
                String temp = "";
                temp += username;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void searchUser(String username) {

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username=?")) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("username"));
                // System.out.println(rs.getString("password")); // Removed sensitive log
            }

            Thread.sleep(1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String cmd) {

        Process process = null;
        BufferedReader br = null;
        try {
            process = Runtime.getRuntime().exec(cmd);

            br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
            } catch (Exception e) { /* ignore */ }
            if (process != null) {
                process.destroy();
            }
        }
    }

    public static String sha256(String text) {

        try {

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();

            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}