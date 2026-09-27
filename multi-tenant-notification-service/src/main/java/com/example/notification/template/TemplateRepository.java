package com.example.notification.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface TemplateRepository extends JpaRepository<Template, UUID> {
    List<Template> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Template> findByIdAndTenantId(UUID id, UUID tenantId);
}
