import java.sql.*;
import java.util.*;

public class JavaTestCode {

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    static {
        if (DB_URL == null || USER == null || PASSWORD == null) {
            throw new IllegalStateException("Required environment variables DB_URL, DB_USER, or DB_PASSWORD are missing");
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String inputPassword = scanner.nextLine();

            System.out.println("Credentials entered.");

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
                String sql = "SELECT * FROM users WHERE username=? AND password=?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, inputPassword);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            System.out.println("Login successful!");

                            List<String> names = new ArrayList<>();
                            String sql2 = "SELECT username FROM users";
                            try (PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                                                                ResultSet rs2 = pstmt2.executeQuery()) {
                                                                   while (rs2.next()) {
                                                                       names.add(rs2.getString("username"));
                                                                   } }
                            }

                            StringBuilder output = new StringBuilder();
                            for (int i = 0; i < names.size(); i++) {
                                output.append(names.get(i));
                                if (i < names.size() - 1) {
                                    output.append(",");
                                }
                            }
                            System.out.println(output.toString());
                        } else {
                            System.out.println("Login failed.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("An error occurred during execution.");
        }
    }
}