package com.example.test_spring.event;

public record AuthEvent(String type, String userId, String email, String occurredAt) {}
