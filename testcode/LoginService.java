import java.sql.*;

public class LoginService {

    private static final String API_KEY = System.getenv("API_KEY");
    private static final String DB_URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    public static void main(String[] args) {
        login("admin", "1234");
    }

    public static void login(String id, String pw) {
        if (API_KEY == null || DB_URL == null || USER == null || PASSWORD == null) {
            throw new IllegalStateException("Missing required environment variables");
        }
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            String sql = "SELECT * FROM users WHERE id = ? AND password = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            pstmt.setString(2, pw);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Login Success");
                System.out.println("API KEY : " + API_KEY);

                StringBuilder result = new StringBuilder();
                for (int i = 0; i < 3000; i++) {
                    result.append(rs.getString("id"));
                }

                System.out.println(result.length());
            } else {
                System.out.println("Login Fail");
            }

            String sql2 = "SELECT * FROM users";
            PreparedStatement pstmt2 = conn.prepareStatement(sql2);
            ResultSet rs2 = pstmt2.executeQuery();

            while (rs2.next()) {
                System.out.println(rs2.getString("id"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}