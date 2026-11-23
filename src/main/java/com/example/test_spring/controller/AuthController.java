package com.example.test_spring.controller;

import com.example.test_spring.DTO.Request.ChangePasswordRequest;
import com.example.test_spring.DTO.Request.LoginRequest;
import com.example.test_spring.DTO.Request.RegisterRequest;
import com.example.test_spring.DTO.Response.LoginResponse;
import com.example.test_spring.model.UserModel;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.test_spring.service.AuthService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @GetMapping
    public List<UserModel> getAllUsers(){
        return authService.getAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/regist-user")
    public String RegistUser(@RequestBody RegisterRequest req, Authentication authentication) {
        Claims claims = (Claims) authentication.getPrincipal();
        String adminId = claims.get("userId", String.class);
        return authService.RegistUser(req, adminId);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return authService.Login(req);
    }

    @PutMapping("/change-password")
    public String updatePassword(@RequestBody ChangePasswordRequest req, Authentication authentication) {
        Claims claims = (Claims) authentication.getPrincipal();
        String userId = claims.get("userId", String.class);
        return authService.ChangePassword(userId, req);
    }
}
