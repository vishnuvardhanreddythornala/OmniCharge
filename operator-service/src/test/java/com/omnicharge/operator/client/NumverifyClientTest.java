package com.omnicharge.operator.client;

import com.omnicharge.operator.common.logging.LogEventPublisher;
import com.omnicharge.operator.dto.NumverifyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class NumverifyClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private NumverifyClient numverifyClient;

    @Test
    void detectOperator_Success() {
        NumverifyResponse response = new NumverifyResponse();
        response.setCarrier("Jio");
        response.setValid(true);

        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                .thenReturn(response);

        NumverifyResponse result = numverifyClient.detectOperator("9876543210");

        assertNotNull(result);
        assertEquals("Jio", result.getCarrier());
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }

    @Test
    void detectOperator_NullResponse() {
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                .thenReturn(null);

        NumverifyResponse result = numverifyClient.detectOperator("9876543210");

        assertNull(result);
    }

    @Test
    void detectOperator_ApiFailure() {
        when(restTemplate.getForObject(anyString(), eq(NumverifyResponse.class)))
                .thenThrow(new RuntimeException("API timeout"));

        NumverifyResponse result = numverifyClient.detectOperator("9876543210");

        assertNull(result);
        verify(logEventPublisher, atLeastOnce()).publish(any());
    }
}
