package com.example.notification.channel;
import com.example.notification.notification.Notification;
public interface NotificationChannel{ChannelType type(); DeliveryResult send(Notification notification,String renderedSubject,String renderedBody);}
