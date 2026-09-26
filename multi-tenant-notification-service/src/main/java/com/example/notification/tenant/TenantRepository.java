package com.example.notification.tenant;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TenantRepository extends JpaRepository<Tenant, UUID>{Optional<Tenant> findByName(String name);}
