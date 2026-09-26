package com.example.notification.security;
import com.example.notification.tenant.Role; import java.util.*;
public record CurrentUser(String externalId, UUID tenantId, Role role) {
    public boolean isPlatformAdmin() {
        return role==Role.PLATFORM_ADMIN;
    }
}
