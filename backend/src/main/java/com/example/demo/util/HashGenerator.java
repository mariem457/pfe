package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123"; // بدّلها بكلمة السر اللي تحبها
        String hash = encoder.encode(rawPassword);
        System.out.println(hash);
    }
}
