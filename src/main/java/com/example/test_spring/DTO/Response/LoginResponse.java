package com.example.test_spring.DTO.Response;

import lombok.Data;

@Data
public class LoginResponse extends BaseResponse{
    private String token;
    private String type;
    private long ttl;
}
