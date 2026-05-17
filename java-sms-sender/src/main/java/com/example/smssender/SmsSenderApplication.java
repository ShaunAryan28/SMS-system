package com.example.smssender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmsSenderApplication {
    public static void main(String[] args) {
        // This line fires up the entire Spring Boot server
        SpringApplication.run(SmsSenderApplication.class, args);
    }
}