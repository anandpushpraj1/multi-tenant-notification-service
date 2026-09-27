package com.example.notification.channel;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "channel_configs",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_channel_config_tenant_type",
                        columnNames = {"tenant_id", "channel"}))
public class ChannelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(columnDefinition = "TEXT")
    private String configJson;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ChannelConfig() {}

    public ChannelConfig(UUID tenantId, ChannelType channel, String configJson) {
        this.tenantId = tenantId;
        this.channel = channel;
        this.configJson = configJson;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public ChannelType getChannel() {
        return channel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean e) {
        enabled = e;
    }

    public String getConfigJson() {
        return configJson;
    }
}
