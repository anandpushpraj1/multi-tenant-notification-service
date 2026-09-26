package com.example.notification.tenant;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AppUserRepository extends JpaRepository<AppUser, UUID>{Optional<AppUser> findByExternalId(String externalId);}
