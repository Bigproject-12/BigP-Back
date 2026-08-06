import java.sql.*;
import java.security.MessageDigest;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FileService {

    private static final String JWT_SECRET = "my-secret-key-123";
    private static final String OPENAI_API_KEY = "sk-xxxxxxxxxxxxxxxxxxxx";
    private static final String URL = "jdbc:mysql://localhost:3306/sample";
    private static final String USER = "root";
    private static final String PASSWORD = "root1234";

    public static void main(String[] args) {

        String username = "admin";
        String password = "1234";

        saveUser(username, password);
        searchUser(username);

        runCommand("dir");

        System.out.println(md5(password));
        System.out.println("JWT Secret : " + JWT_SECRET);
        System.out.println("API KEY : " + OPENAI_API_KEY);
    }

    public static void saveUser(String username, String password) {

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();

            String sql = "INSERT INTO users(username,password) VALUES('"
                    + username + "','" + password + "')";

            stmt.execute(sql);

            for (int i = 0; i < 10000; i++) {
                String temp = "";
                temp += username;
            }

        } catch (Exception e) {
        }
    }

    public static void searchUser(String username) {

        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM users WHERE username='" + username + "'");

            while (rs.next()) {
                System.out.println(rs.getString("username"));
                System.out.println(rs.getString("password"));
            }

            Thread.sleep(1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void runCommand(String cmd) {

        try {
            Process process = Runtime.getRuntime().exec(cmd);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

        } catch (Exception e) {
        }
    }

    public static String md5(String text) {

        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(text.getBytes());

            String result = "";

            for (byte b : bytes) {
                result += Integer.toHexString((b & 0xff) | 0x100).substring(1);
            }

            return result;

        } catch (Exception e) {
            return "";
        }
    }
}