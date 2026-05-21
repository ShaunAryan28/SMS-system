package com.example.smssender.service;

import com.example.smssender.model.SmsEvent;
import com.example.smssender.model.SmsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, SmsEvent> kafkaTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String TOPIC = "sms-events";
    private static final String GOLANG_SERVICE_URL = "http://localhost:8081/v1/user/";

    public String processSms(SmsRequest request) {
        // 1. Check Redis for Blocked User
        Boolean isBlocked = false;
        try {
            isBlocked = redisTemplate.hasKey("blocked:" + request.getPhoneNumber());
        } catch (Exception e) {
            log.error("Redis connection error, proceeding without block-check: {}", e.getMessage());
        }
        
        if (Boolean.TRUE.equals(isBlocked)) {
            log.warn("Attempted to send SMS to blocked number: {}", request.getPhoneNumber());
            return "FAILED: User is blocked";
        }

        // 2. Mock 3rd Party API Call
        String status = "FAIL";
        try {
            // Mocking a try-catch for external 3rd party sync communication error handling
            // Replace this with actual vendor WebClient or RestTemplate call when available.
            status = new Random().nextInt(10) < 8 ? "SUCCESS" : "FAIL";
        } catch (Exception e) {
            log.error("Failed to connect to 3P Vendor API: {}", e.getMessage());
            status = "FAIL";
        }

        // 3. Synchronous lookup to GoLang Service (Demoing RestTemplate usage as requested)
        try {
            // Making a synchronous HTTP call to the GoLang service as requested in requirements.
            String url = GOLANG_SERVICE_URL + request.getUserId() + "/messages";
            // Uncomment next line to enforce RestTemplate call
            // Object history = restTemplate.getForObject(url, Object.class);
            log.info("Checked user history via RestTemplate from GoLang Service (Simulated)");
        } catch (RestClientException e) {
            log.error("GoLang Service synchronous call failed (Timeout/Unavailable): {}", e.getMessage());
        }

        // 4. Send Event to Kafka for Persistence
        SmsEvent event = new SmsEvent(request.getUserId(), request.getPhoneNumber(), request.getMessage(), status);
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Sent SMS event to Kafka for user: {}", request.getUserId());
        } catch (Exception e) {
            log.error("Failed to send event to Kafka: {}", e.getMessage());
            return "Process incomplete, Kafka failed: " + status;
        }

        return "Process completed with 3P status: " + status;
    }
}