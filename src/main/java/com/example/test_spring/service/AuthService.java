package com.example.test_spring.service;

import com.example.test_spring.DTO.Both.CredentialCacheDTO;
import com.example.test_spring.DTO.Request.ChangePasswordRequest;
import com.example.test_spring.DTO.Request.LoginRequest;
import com.example.test_spring.DTO.Request.RegisterRequest;
import com.example.test_spring.DTO.Response.LoginResponse;
import com.example.test_spring.event.EventMessage;
import com.example.test_spring.event.RegistEvent;
import com.example.test_spring.helper.HashHelper;
import com.example.test_spring.model.RoleModel;
import com.example.test_spring.model.UserModel;
import com.example.test_spring.repository.RoleRepository;
import com.example.test_spring.event.EventPublisher;
import com.example.test_spring.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EventPublisher eventPublisher;
    private final JWTService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final HashHelper hash;
    private final CredentialCacheService credentialCache;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public List<UserModel> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public String RegistUser(RegisterRequest req, String adminId) {
        try {
            if (userRepository.existsByEmail(req.getEmail())) {
                return "Email already registered";
            }

            Optional<RoleModel> role = roleRepository.findById(req.getRoleId());
            if (role.isEmpty()) return "Role not found";

            UserModel newUser = new UserModel();
            newUser.setFullName(req.getFullName());
            newUser.setUserName(req.getUserName());
            newUser.setEmail(req.getEmail());
            newUser.setPassword(hash.md5(req.getPassword()));
            newUser.setRole(role.get());

            try {
                UserModel savedUser = userRepository.save(newUser);

                eventPublisher.publish("auth.user.registered",
                        EventMessage.of("user_registration", adminId,
                                new RegistEvent(
                                        savedUser.getIdUser().toString(),
                                        savedUser.getFullName(),
                                        savedUser.getEmail(),
                                        savedUser.getRole().getId()
                                )));

                return "Added user successfully " + savedUser.getEmail();
            } catch (DataIntegrityViolationException ex) {
                return "Data not valid or duplicated";
            }
        } catch (Exception e) {
            return "Something Happened: " + e.getMessage();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse Login(LoginRequest req) {
        LoginResponse res = new LoginResponse();
        try {
            boolean userDbFound = false;
            CredentialCacheDTO cred = credentialCache.get(req.getEmail()).orElse(null);

            if (cred == null) {
                UserModel user = userRepository.findByEmail(req.getEmail());

                if (user == null) {
                    res.setStatus(false);
                    res.setMessage("Email not found");
                    return res;
                }

                if (!hash.SameHash(hash.md5(req.getPassword()), user.getPassword())) {
                    res.setStatus(false);
                    res.setMessage("Invalid Credentials");
                    return res;
                }

                userDbFound = true;
                cred = new CredentialCacheDTO();
                cred.setIdUser(user.getIdUser().toString());
                cred.setEmail(user.getEmail());
                cred.setPassword(user.getPassword());
                cred.setRole(user.getRole().getName());

                credentialCache.put(cred);
            }

            if (!userDbFound && !hash.SameHash(hash.md5(req.getPassword()), cred.getPassword())) {
                res.setStatus(false);
                res.setMessage("Invalid Credentials");
                return res;
            }

            String jti = UUID.randomUUID().toString();
            String token = jwtService.GenerateToken(cred, jti);

            res.setStatus(true);
            res.setToken(token);
            res.setType("Bearer");
            res.setTtl(expirationMs/1000);
            return res;
        } catch (Exception e){
            res.setMessage("Something Happened:" + e.getMessage());
            res.setStatus(false);
            return res;
        }
    }

    @Transactional
    public String ChangePassword(String userId, ChangePasswordRequest req) {
        Optional<UserModel> user = userRepository.findById(UUID.fromString(userId));

        if (user.isEmpty()) return "User not found";

        UserModel userFound = user.get();
        if (!hash.SameHash(hash.md5(req.getOldPassword()), userFound.getPassword())) return "Invalid old password";
        if (req.getOldPassword().equals(req.getNewPassword())) return "New password can't be same like the old one";

        String newHash = hash.md5(req.getNewPassword());
        userFound.setPassword(newHash);
        userRepository.save(userFound);

        CredentialCacheDTO cred = credentialCache.get(userFound.getEmail()).orElse(null);
        if (cred == null) return "Credentials not found";

        cred.setIdUser(userFound.getIdUser().toString());
        cred.setEmail(userFound.getEmail());
        cred.setPassword(newHash);
        cred.setRole(userFound.getRole().getName());
        credentialCache.put(cred);

        return "Password changed";
    }
}