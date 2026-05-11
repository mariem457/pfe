package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableIntegration
@EnableScheduling
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.example.demo")
public class BackendWisetrashApplication {

    public static void main(String[] args) {
        System.out.println("🚀 BACKEND WISETRASH STARTED");
        SpringApplication.run(BackendWisetrashApplication.class, args);
    }
}