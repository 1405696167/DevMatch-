package com.devmatch.security;

import com.devmatch.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static SecurityUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SecurityUser su) {
            return su;
        }
        throw new BusinessException(401, "未登录");
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentRole() {
        return getCurrentUser().getRole();
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentRole());
    }

    public static boolean isDeveloper() {
        return "DEVELOPER".equals(getCurrentRole());
    }

    public static boolean isEnterprise() {
        return "ENTERPRISE".equals(getCurrentRole());
    }
}
