import java.sql.*;
import java.util.*;
import com.aivle.bigproject.ai.dto.IndexRepoResponse;
import com.aivle.bigproject.service.GithubService;

public class JavaTestCode {
    static String URL = "jdbc:mysql://localhost:3306/test";
    static String USER = "root";
    static String PASSWORD = "1234";
    static GithubService githubService = new GithubService();

    public static void login(String username, String password) {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                System.out.println("Login Success");
            } else {
                System.out.println("Login Failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String command) throws Exception {
        // Command Injection 가능
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
    }

    public static List<String> removeDuplicates(List<String> list) {
        return new ArrayList<>(new HashSet<>(list));
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