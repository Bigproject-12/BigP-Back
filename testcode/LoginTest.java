import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        log = log + ":"" + System.currentTimeMillis();

        int result = 0;
        for(int i=0;i<id.length();i++){
            for(int j=0;j<pw.length();j++){
                result += id.charAt(i);
                result += pw.charAt(j);
            }
        }

        String password = pw;

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/test",
                "root",
                "1234")) {
            
            String sql = "SELECT * FROM users WHERE id=? AND password=";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, id);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        // 결과셋 처리 로직 필요 시 추가
                    }
                }
            }
        }

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