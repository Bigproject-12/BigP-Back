import java.sql.*;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FileService {

    private static final String JWT_SECRET = getEnv("JWT_SECRET");
    private static final String OPENAI_API_KEY = getEnv("OPENAI_API_KEY");
    private static final String URL = getEnv("DB_URL");
    private static final String USER = getEnv("DB_USER");
    private static final String PASSWORD = getEnv("DB_PASSWORD");

    private static String getEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Environment variable " + key + " is is not set");
        }
        return value;
    }

    public static void main(String[] args) {
        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);

        runCommand("dir");

        System.out.println(sha256(password));
        // Removed logging of secrets and API keys
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
            // Handle or log properly
        }
    }

    public static void searchUser(String username) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            String sql = "SELECT * FROM users WHERE username=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getString("username"));
                // Removed password logging
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
            // Handle error
        }
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes());
            String result = "";
            for (byte b : bytes) {
                result += Integer.toHexString((b & 0xff) | 0x100).substring(1);
            }
            return result;
        } catch (Exception e) {
            return "";
        }
    }
}