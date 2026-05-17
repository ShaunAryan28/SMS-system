package com.example.smssender.service;

import com.example.smssender.model.SmsEvent;
import com.example.smssender.model.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, SmsEvent> kafkaTemplate;
    private static final String TOPIC = "sms-events";

    public String processSms(SmsRequest request) {
        // 1. Check Redis for Blocked User
        Boolean isBlocked = redisTemplate.hasKey("blocked:" + request.getPhoneNumber());
        if (Boolean.TRUE.equals(isBlocked)) {
            log.warn("Attempted to send SMS to blocked number: {}", request.getPhoneNumber());
            return "FAILED: User is blocked";
        }

        // 2. Mock 3rd Party API Call
        String status = new Random().nextInt(10) < 8 ? "SUCCESS" : "FAIL";

        // 3. Send Event to Kafka
        SmsEvent event = new SmsEvent(request.getUserId(), request.getPhoneNumber(), request.getMessage(), status);
        kafkaTemplate.send(TOPIC, event);
        log.info("Sent SMS event to Kafka for user: {}", request.getUserId());

        return "Process completed with 3P status: " + status;
    }
}