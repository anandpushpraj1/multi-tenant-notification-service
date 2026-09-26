package com.example.notification.security;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;

public final class SecurityUtils {
    private SecurityUtils() {

    }

    public static CurrentUser current() {
        Object p=SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (CurrentUser)p;
    }

    public static UUID tenant() {
        return current().tenantId();
    }
}
