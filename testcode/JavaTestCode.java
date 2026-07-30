import java.sql.*;
import java.util.*;

public class JavaTestCode {
    static String URL = "jdbc:mysql://localhost:3306/test";
    static String USER = "root";
    static String PASSWORD = "1234";

    public static void login(String username, String password) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // Prepared Statement 사용
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Login Success");
                    } else {
                        System.out.println("Login Failed");
                    }
                }
            }
        } catch (SQLException e) {
            // 민감한 정보 노출
            e.printStackTrace();
        }
    }

    public static void runCommand(String command) throws Exception {
        // Command Injection 방지
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }

    public static List<String> removeDuplicates(List<String> list) {
        // Set을 사용한 중복 제거 (O(n))
        Set<String> set = new HashSet<>(list);
        return new ArrayList<>(set);
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        String username = sc.nextLine();
        login(username, "password"); // password는 실제 로그인 시 사용하는 비밀번호와 다를 수 있습니다.
    }
}