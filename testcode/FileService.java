import java.sql.*;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FileService {
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    static {
        if (URL == null || USER == null || PASSWORD == null) {
            throw new IllegalException("Environment variables DB_URL, DB_USER, DB_PASSWORD must be set");
        }
    }

    public static void main(String[] args) {
        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);
        runCommand("dir");
        System.out.println(md5(password));
    }

    public static void saveUser(String username, String password) {
        String sql = "INSERT INTO users(username,password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void searchUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getString("username"));
                    // Sensitive data not logged
                    System.out.println("password found");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String cmd) {
        try (Process process = Runtime.getRuntime().exec(cmd);
             BufferedReader br = new BufferedReader(
                     new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(text.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(Integer.toHexString((b & 0xff) | 0x100).substring(1));
            }
            return result.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static class IllegalException extends RuntimeException {
        public IllegalException(String message) {
            super(message);
        }
    }
}