package com.example.smssender.model;
import lombok.Data;

@Data
public class SmsRequest {
    private String userId;
    private String phoneNumber;
    private String message;
}