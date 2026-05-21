package com.example.smssender.service;

import com.example.smssender.model.SmsEvent;
import com.example.smssender.model.SmsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SmsServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private KafkaTemplate<String, SmsEvent> kafkaTemplate;

    @InjectMocks
    private SmsService smsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessSms_UserBlocked() {
        SmsRequest request = new SmsRequest();
        request.setUserId("user1");
        request.setPhoneNumber("+1234");
        request.setMessage("Hello");

        when(redisTemplate.hasKey("blocked:+1234")).thenReturn(true);

        String result = smsService.processSms(request);

        assertTrue(result.contains("FAILED: User is blocked"));
        verify(kafkaTemplate, never()).send(anyString(), any(SmsEvent.class));
    }

    @Test
    void testProcessSms_Success() {
        SmsRequest request = new SmsRequest();
        request.setUserId("user1");
        request.setPhoneNumber("+1234");
        request.setMessage("Hello");

        when(redisTemplate.hasKey("blocked:+1234")).thenReturn(false);

        String result = smsService.processSms(request);

        assertTrue(result.contains("SUCCESS") || result.contains("FAIL"));
        verify(kafkaTemplate, times(1)).send(eq("sms-events"), any(SmsEvent.class));
    }
}
