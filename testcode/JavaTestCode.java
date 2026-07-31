import java.sql.*;
import java.util.*;




public class JavaTestCode {
    static String URL = "jdbc:mysql://localhost:3306/test";
    static String USER = "root";
    static String PASSWORD = "1234";

    public static void login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();
            // SQL Injection 취약점
            String sql = "SELECT * FROM users WHERE username='" +
                    username + "' AND password='" + password + "'";
            ResultSet rs = stmt.executeQuery(sql);
            // 불필요하게 같은 쿼리 재실행
            stmt.executeQuery(sql);
            if (rs.next()) {
                System.out.println("Login Success");
            } else {
                System.out.println("Login Failed");
            }


        } catch (Exception e) {
            // 민감한 정보 노출
            e.printStackTrace();
        }
    }

    public static void runCommand(String command) throws Exception {
        // Command Injection 가능
        Runtime.getRuntime().exec(command);


    }

    public static List<String> removeDuplicates(List<String> list) {
        List<String> result = new ArrayList<>();
        // 비효율적인 O(n²) 중복 제거
        for (String item : list) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String username = sc.nextLine();
    }
}