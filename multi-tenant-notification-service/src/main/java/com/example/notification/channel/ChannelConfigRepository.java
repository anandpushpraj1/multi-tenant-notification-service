package com.example.notification.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, UUID> {
    List<ChannelConfig> findByTenantId(UUID tenantId);

    Optional<ChannelConfig> findByTenantIdAndChannel(UUID tenantId, ChannelType channel);
}
