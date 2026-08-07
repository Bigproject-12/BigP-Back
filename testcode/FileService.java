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

    static {
        if (JWT_SECRET == null || OPENAI_API_KEY == null || URL == null || USER == null || PASSWORD == null) {
            throw new IllegalStateException("Required environment variables are not set");
        }
    }

    public static void main(String[] args) {

        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);

        runCommand("dir");

        System.out.println(sha256(password));
        System.out.println("JWT Secret : masked");
        System.out.println("API KEY : masked");
    }

    public static void saveUser(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "INSERT INTO users(username,password) VALUES(?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.execute();

            for (int i = 0; i < 10000; i++) {
                String temp = "";
                temp += username;
            }
        } catch (Exception e) {
            // Log error
        }
    }

    public static void searchUser(String username) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "SELECT username, password FROM users WHERE username=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("username"));
                // Avoid logging password
            }

            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            // Log error
        }
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(Integer.toHexString((b & 0xff) | 0x10).substring(1));
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }
}