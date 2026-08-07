import java.sql.*;

public class LoginService {

    private static final String API_KEY = "sk-test-abcdefghijklmnop";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        login("admin", "1234");
    }

    public static void login(String id, String pw) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            String sql = "SELECT * FROM users WHERE id='" + id +
                         "' AND password='" + pw + "'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                System.out.println("Login Success");
                System.out.println("API KEY : " + API_KEY);

                String result = "";
                for (int i = 0; i < 3000; i++) {
                    result += rs.getString("id");
                }

                System.out.println(result.length());
            } else {
                System.out.println("Login Fail");
            }

            Statement stmt2 = conn.createStatement();
            ResultSet rs2 = stmt2.executeQuery("SELECT * FROM users");

            while (rs2.next()) {
                System.out.println(rs2.getString("id"));
            }

        } catch (Exception e) {
        }
    }
}