package com.example.notification.channel;

public record DeliveryResult(
        boolean success,
        boolean transientFailure,
        String providerResponse,
        String errorCode,
        String errorMessage) {
    public static DeliveryResult success(String r) {
        return new DeliveryResult(true, false, r, null, null);
    }

    public static DeliveryResult transientFailure(String c, String m) {
        return new DeliveryResult(false, true, null, c, m);
    }

    public static DeliveryResult permanentFailure(String c, String m) {
        return new DeliveryResult(false, false, null, c, m);
    }
}
