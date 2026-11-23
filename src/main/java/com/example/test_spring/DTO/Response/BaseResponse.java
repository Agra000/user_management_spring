package com.example.test_spring.DTO.Response;

import lombok.Data;

@Data
public class BaseResponse {
    private boolean status;
    private String message;
}
