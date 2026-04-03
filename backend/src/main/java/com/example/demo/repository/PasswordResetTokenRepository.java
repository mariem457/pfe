package com.example.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.PasswordResetToken;
import com.example.demo.entity.User;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);



    Optional<PasswordResetToken> findByTokenAndUsedFalseAndUser_Email(String token, String email);

    void deleteByUser(User user);
}