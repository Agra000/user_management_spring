package com.example.test_spring.event;

public record RegistEvent(
        String id,
        String fullName,
        String email,
        Integer roleId
) {}