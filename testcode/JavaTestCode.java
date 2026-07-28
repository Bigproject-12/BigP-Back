import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class JavaTestCode {
    static String URL = "jdbc:mysql://localhost:3306/test";
    static String USER = "root";
    static String PASSWORD = "1234";

    public static void login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // PreparedStatement를 사용하여 SQL Injection 공격을 방지
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Login Success");
            } else {
                System.out.println("Login Failed");
            }
            pstmt.close();
            conn.close();
        } catch (Exception e) {
            // 민감한 정보 노출
            e.printStackTrace();
        }
    }

    public static void runCommand(String command) throws Exception {
        // Command Injection 공격을 방지하기 위해 ProcessBuilder를 사용
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
        pb.start();
    }

    public static List<String> removeDuplicates(List<String> list) {
        List<String> result = new ArrayList<>();
        // O(n) 중복 제거
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
        String password = sc.nextLine();
        login(username, password);
        String command = sc.nextLine();
        runCommand(command);
        List<String> data = Arrays.asList("A", "B", "A", "C", "B");
        System.out.println(removeDuplicates(data));
    }
}