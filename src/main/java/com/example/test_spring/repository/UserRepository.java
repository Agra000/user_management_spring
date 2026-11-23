package com.example.test_spring.repository;

import com.example.test_spring.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserModel, UUID> {
    Boolean existsByEmail(String email);
    UserModel findByEmail(String email);
}