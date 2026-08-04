import java.sql.*;
import java.util.*;

public class JavaTestCode {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/test";
    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        System.out.println("Password entered: " + password);

        try {
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);

            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                System.out.println("Login successful!");

                List<String> names = new ArrayList<>();
                String sql2 = "SELECT username FROM users";
                PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                ResultSet rs2 = pstmt2.executeQuery();

                while (rs2.next()) {
                    names.add(rs2.getString("username"));









                }

                String output = "";
                for (String name : names) {
                    output += name + ",";
                }

                System.out.println(output);
            } else {
                System.out.println("Login failed.");
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }