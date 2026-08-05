package com.aivle.bigproject.util;

<<<<<<< HEAD
public class JavaTestCode {

    private static final String DB_URL = System.getenv("DB_URL") != null ? System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/test";
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "1234";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        System.out.println("Password entered: " + password);

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            String sql = "SELECT * FROM users WHERE username=? AND password=?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Login successful!");

                        List<String> names = new ArrayList<>();
                        String sql2 = "SELECT username FROM users";
                        try (PreparedStatement pstmt2 = conn.prepareStatement(sql2);
                             ResultSet rs2 = pstmt2.executeQuery()) {
                                while (rs2.next()) {
                                    names.add(rs2.getString("username"));
                                }
                        }

                                                StringBuilder output = new StringBuilder();
                        for (int i = 0; i < names.size(); i++) {
                            output.append(names.get(i));
                            if (i < names.size() - 1) {
                                output.append(",");
                            }
                        }
                        System.out.println(output.toString());
                    } else {
                        System.out.println("Login failed.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
=======
import com.aivle.bigproject.dto.company.CompanyResponse;
import com.aivle.bigproject.dto.notification.NotificationResponse;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.Notification;
import java.util.List;

public class ResponseMapper {

    public static CompanyResponse mapToCompanyInfo(Company company) {
        return CompanyResponse.from(company);
    }

    public static NotificationResponse mapToNotificationInfo(Notification notification) {
        return NotificationResponse.from(notification);
    }

    public static String determineAccessLevel(
        String role, boolean isActive, boolean isVerified, boolean hasSubscription,
        boolean isTrial, boolean isSuspended, String region, int loginCount, boolean hasPromo) {
        if (isSuspended) {
            return "SUSPENDED";
        }

        if ("MEMBER".equals(role)) {
            if (hasSubscription) {
                if (isTrial) {
                    if ("KR".equals(region)) return "TRIAL_MEMBER_KR";
                    if ("US".equals(region)) return "TRIAL_MEMBER_US";
                    return "TRIAL_MEMBER";
                }
                if ("KR".equals(region)) {
                    return hasPromo ? "PREMIUM_MEMBER_KR_PROMO" : "PREMIUM_MEMBER_KR";
                }
                return "PREMIUM_MEMBER";
            }
            if (loginCount > 100) return "LOYAL_FREE_MEMBER";
            if (loginCount > 50) return "ACTIVE_FREE_MEMBER";
            if (loginCount > 10) return "REGULAR_FREE_MEMBER";
            return "NEW_FREE_MEMBER";
        }

        if ("GUEST_PLUS".equals(role)) {
            return hasPromo ? "GUEST_PROMO" : "GUEST";
        }

        return "GUEST";
    }

    public static List<CompanyResponse> mapToCompanyInfoList(List<Company> companies) {
        return companies.stream()
                .map(ResponseMapper::mapToCompanyInfo)
                .toList();
>>>>>>> e2f14ed9da3ffe713c49b82ff9e92a86a2a02383
    }
}