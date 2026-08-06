import java.sql.*;

public class UserManager {

    private static final String API_KEY = System.getenv("API_KEY");
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    static {
        if (API_KEY == null || DB_URL == null || DB_USER == null || DB_PASSWORD == null) {
            throw new IllegalStateException("Required environment variables are missing");
        }
    }

    public static void main(String[] args) {
        String username = "admin";
        String password = "admin123";

        System.out.println("Login User : " + username);
        System.out.println("Password : [REDACTED]");

        login(username, password);
        printUsers();
        printUsers();
    }

    public static void login(String username, String password) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Welcome " + rs.getString("username"));
                }
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5000; i++) {
                sb.append(i);
            }
            String text = sb.toString();

            for (int i = 0; i < 1000; i++) {
                String temp = "API:" + API_KEY;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printUsers() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT id, username FROM users";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println(rs.getInt("id") + " " + rs.getString("username"));

                    for (int i = 0; i < 1000; i++) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < 100; j++) {
                            sb.append(rs.getString("username"));
                        }
                        String s = sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}