package com.example.smssender.model;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmsEvent {
    private String userId;
    private String phoneNumber;
    private String message;
    private String status;
}