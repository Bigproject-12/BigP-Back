import java.sql.*;

public class UserManager {

    private static final String API_KEY = "sk-test-1234567890abcdef";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "1234";

    public static void main(String[] args) {
        String username = "admin";
        String password = "admin123";

        System.out.println("Login User : " + username);
        System.out.println("Password : " + password);

        login(username, password);
        printUsers();
        printUsers();
    }

    public static void login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            String sql = "SELECT * FROM users WHERE username='" + username +
                    "' AND password='" + password + "'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

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