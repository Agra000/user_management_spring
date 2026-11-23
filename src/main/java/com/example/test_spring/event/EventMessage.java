package com.example.test_spring.event;

import java.time.Instant;
import java.util.UUID;

public record EventMessage<T>(
        String eventId,
        String eventType,
        String createdAt,
        String createdBy,
        T data
) {
    public static <T> EventMessage<T> of(String eventType, String createdBy, T data) {
        return new EventMessage<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                createdBy,
                data
        );
    }
}