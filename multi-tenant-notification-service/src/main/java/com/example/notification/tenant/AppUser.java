package com.example.notification.tenant;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="app_users", uniqueConstraints=@UniqueConstraint(name="uk_user_external", columnNames="external_id"))
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="external_id", nullable=false, length=120) private String externalId;
    @Column(name="tenant_id") private UUID tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private Role role;
    protected AppUser() {}
    public AppUser(String externalId, UUID tenantId, Role role){this.externalId=externalId;this.tenantId=tenantId;this.role=role;}
    public UUID getId(){return id;} public String getExternalId(){return externalId;} public UUID getTenantId(){return tenantId;} public Role getRole(){return role;}
}
