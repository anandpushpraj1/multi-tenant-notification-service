package com.example.notification.channel;

import jakarta.validation.constraints.NotNull;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/channel-configs")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class ChannelConfigController {
    private final ChannelConfigService s;

    public ChannelConfigController(ChannelConfigService s) {
        this.s = s;
    }

    public record Request(@NotNull ChannelType channel, String configJson, boolean enabled) {}

    @PutMapping
    public ChannelConfig upsert(@RequestBody @jakarta.validation.Valid Request r) {
        return s.upsert(r.channel(), r.configJson(), r.enabled());
    }

    @GetMapping
    public List<ChannelConfig> list() {
        return s.list();
    }
}
