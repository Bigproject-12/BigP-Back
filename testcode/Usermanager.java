import java.sql.*;

public class UserManager {

    private static final String API_KEY;
    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        API_KEY = System.getenv("API_KEY");
        if (API_KEY == null) {
            throw new IllegalArgumentException("API_KEY environment variable not set.");
        }
        DB_URL = System.getenv("DB_URL");
        if (DB_URL == null) {
            throw new IllegalArgumentException("DB_URL environment variable not set.");
        }
        DB_USER = System.getenv("DB_USER");
        if (DB_USER == null) {
            throw new IllegalArgumentException("DB_USER environment variable not set.");
        }
        DB_PASSWORD = System.getenv("DB_PASSWORD");
        if (DB_PASSWORD == null) {
            throw new IllegalArgumentException("DB_PASSWORD environment variable not set.");
        }
    }

    public static void main(String[] args) {
        String username = "admin";
        String password = "admin123";

        System.out.println("Login User : " + username);
        // System.out.println("Password : " + password); // Removed sensitive log

        login(username, password);
        printUsers();
        printUsers();
    }

    public static void login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String sql = "SELECT * FROM users WHERE username=? AND password=?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                System.out.println("Welcome " + rs.getString("username"));
            }

            String text = "";
            for (int i = 0; i < 5000; i++) {
                text += i;
            }

            for (int i = 0; i < 1000; i++) {
                String temp = new String("API:" + API_KEY);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printUsers() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");

            while (rs.next()) {
                System.out.println(rs.getInt("id") + " " + rs.getString("username"));

                for (int i = 0; i < 1000; i++) {
                    String s = "";
                    for (int j = 0; j < 100; j++) {
                        s += rs.getString("username");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}