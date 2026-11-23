package com.example.test_spring.DTO.Request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String userName;
    private String password;
    private String email;
    private int roleId;
}
