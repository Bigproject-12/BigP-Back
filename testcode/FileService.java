import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.crypto.Mac;
import java.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class FileService {

    private static final String JWT_SECRET = System.getenv("JWT_SECRET");
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    static {
        if (JWT_SECRET == null || OPENAI_API_KEY == null || URL == null || USER == null || PASSWORD == null) {
            throw new IllegalStateException("Required environment variables are missing");
        }
    }

    public static void main(String[] args) {

        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);

        runCommand("dir");

        System.out.println(hmacSha256(password, JWT_SECRET));
        System.out.println("JWT Secret : [REDACTED]");
        System.out.println("API KEY : [REDACTED]");
    }

    public static void saveUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "INSERT INTO users(username,password) VALUES(?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.execute();
            }
            for (int i = 0; i < 10000; i++) {
                String temp = "";
                temp += username;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void searchUser(String username) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String sql = "SELECT * FROM users WHERE username=?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println(rs.getString("username"));
                        System.out.println("[REDACTED]");
                    }
                }
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
        }
    }

    public static String hmacSha256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC", e);
        }
    }
}
}