package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.DuplicateResourceException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.internet.MimeMessage;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private JavaMailSender javaMailSender;
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@ex.com");
        sampleUser.setIsEmailVerified(false);
    }

    @Test
    void sendVerificationOtp_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailVerificationService.sendVerificationOtp(1L, "new@ex.com");

        verify(valueOperations, times(1)).set(eq("email-verify:1"), anyString(), eq(10L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(1)).set(eq("email-verify-address:1"), eq("new@ex.com"), eq(10L), eq(TimeUnit.MINUTES));
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void sendVerificationOtp_DuplicateEmail() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("new@ex.com");
        otherUser.setIsEmailVerified(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> emailVerificationService.sendVerificationOtp(1L, "new@ex.com"));
    }

    @Test
    void sendVerificationOtp_MailFailureThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        
        doThrow(new RuntimeException("SMTP down")).when(javaMailSender).send(mimeMessage);

        assertThrows(BadRequestException.class, () -> emailVerificationService.sendVerificationOtp(1L, "new@ex.com"));
    }

    @Test
    void verifyEmail_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email-verify:1")).thenReturn("123456");
        when(valueOperations.get("email-verify-address:1")).thenReturn("new@ex.com");
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(false);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailVerificationService.verifyEmail(1L, "123456");

        verify(userRepository, times(1)).save(sampleUser);
        verify(redisTemplate, times(1)).delete("email-verify:1");
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void verifyEmail_InvalidOtp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email-verify:1")).thenReturn("123456");
        when(valueOperations.get("email-verify-address:1")).thenReturn("new@ex.com");

        assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail(1L, "000000"));
    }

    @Test
    void verifyEmail_ExpiredOtp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email-verify:1")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> emailVerificationService.verifyEmail(1L, "123456"));
    }

    @Test
    void verifyEmail_AnotherUserClaimedItWhilePending() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("new@ex.com");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email-verify:1")).thenReturn("123456");
        when(valueOperations.get("email-verify-address:1")).thenReturn("new@ex.com");
        when(userRepository.existsByEmail("new@ex.com")).thenReturn(true);
        when(userRepository.findByEmail("new@ex.com")).thenReturn(Optional.of(otherUser));

        assertThrows(DuplicateResourceException.class, () -> emailVerificationService.verifyEmail(1L, "123456"));
    }
}
