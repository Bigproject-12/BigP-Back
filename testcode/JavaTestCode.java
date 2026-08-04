import java.sql.*;
import java.util.*;

public class UserManager {

    private static final String DB_URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/test";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "1234";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.println("Password entered: " + password);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Login successful!");

                        List<String> names = new ArrayList<>();
                        String sql2 = "SELECT username FROM users";
                        try (PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                             ResultSet rs2 = pstmt2.executeQuery()) {
                            while (rs2.next()) {
                                names.add(rs2.getString("username"));
                            }
                        }

                        StringBuilder output = new StringBuilder();
                        for (String name : names) {
                            output.append(name).append(",");
                        }
                        System.out.println(output.toString());
                    } else {
                        System.out.println("Login failed.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}