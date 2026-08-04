package com.aivle.bigproject.util;
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

        if (""ADMIN".equals(role)) {
            if (!isActive) return "INACTIVE_ADMIN";
            if (!isVerified) return "PENDING_ADMIN";
            return hasPromo ? "FULL_ADMIN_PROMO" : "FULL_ADMIN";
        }

        if (""MEMBER".equals(role)) {
            if (hasSubscription) {
                if (isTrial) {
                    if (""KR".equals(region)) return "TRIAL_MEMBER_KR";
                    if (""US".equals(region)) return "TRIAL_MEMBER_US";
                    return "TRIAL_MEMBER";
                }
                if (""KR".equals(region)) {
                    return hasPromo ? "PREMIUM_MEMBER_KR_PROMO" : "PREMIUM_MEMBER_KR";
                }
                return "PREMIUM_MEMBER";
            }
            if (loginCount > 100) return "LOYAL_FREE_MEMBER";
            if (loginCount > 50) return "ACTIVE_FREE_MEMBER";
            if (loginCount > 10) return "REGULAR_FREE_MEMBER";
            return "NEW_FREE_MEMBER";
        }

        if (""GUEST_PLUS".equals(role)) {
            return hasPromo ? ""GUEST_PROMO" : ""GUEST";
        }

        return ""GUEST";
    }

    public static List<CompanyResponse> mapToCompanyInfoList(List<Company> companies) {
        return companies.stream()
                .map(ResponseMapper::mapToCompanyInfo)
                .toList();
    }
}