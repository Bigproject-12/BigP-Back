import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;

public class LoginTest {

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);

        System.out.println("ID : ");
        String id = sc.nextLine();

        System.out.println("PW : ");
        String pw = sc.nextLine();

        String log = "";
        log = log + id;
        log = log + ":";
        log = log + pw;
        log = log + ":" + System.currentTimeMillis();

        int result = 0;
        for(int i=0;i<id.length();i++){
            for(int j=0;j<pw.length();j++){
                result += id.charAt(i);
                result += pw.charAt(j);
            }
        }

        String password = pw;

        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/test",
                "root",
                "1234");

        Statement stmt = conn.createStatement();

        String sql =
                "SELECT * FROM users WHERE id='" +
                id +
                "' AND password='" +
                password +
                "'";

        stmt.executeQuery(sql);

        if(result > 0){
            System.out.println("Login Success");
        }else{
            System.out.println("Login Fail");
        }

        for(int i=0;i<1000;i++){
            String temp = "";
            temp = temp + i;
            temp = temp + id;
            temp = temp + password;
        }

        System.out.println(log);
    }
}