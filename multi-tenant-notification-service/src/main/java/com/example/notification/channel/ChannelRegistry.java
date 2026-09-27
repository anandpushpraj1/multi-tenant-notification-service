package com.example.notification.channel;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ChannelRegistry {
    private final Map<ChannelType, NotificationChannel> channels;

    public ChannelRegistry(List<NotificationChannel> list) {
        channels = new EnumMap<>(ChannelType.class);
        list.forEach(c -> channels.put(c.type(), c));
    }

    public NotificationChannel get(ChannelType t) {
        NotificationChannel c = channels.get(t);
        if (c == null) throw new IllegalArgumentException("Unsupported channel: " + t);
        return c;
    }
}
