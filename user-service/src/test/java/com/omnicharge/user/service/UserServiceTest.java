package com.omnicharge.user.service;

import com.omnicharge.user.common.exception.BadRequestException;
import com.omnicharge.user.common.exception.ResourceNotFoundException;
import com.omnicharge.user.common.exception.UnauthorizedException;
import com.omnicharge.user.common.logging.LogEvent;
import com.omnicharge.user.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LogEventPublisher logEventPublisher;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("test@ex.com");
        sampleUser.setFullName("Test User");
        sampleUser.setPassword("encoded");
        sampleUser.setRole(Role.ROLE_USER);
        sampleUser.setIsActive(true);
        sampleUser.setAuthProvider(AuthProvider.LOCAL);
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        UserProfileResponse response = userService.getProfile(1L);
        assertNotNull(response);
        assertEquals("test@ex.com", response.getEmail());
    }

    @Test
    void getProfile_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(1L));
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse resp = userService.updateProfile(1L, req);

        assertEquals("Updated Name", resp.getFullName());
        verify(userRepository, times(1)).save(sampleUser);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass");

        RefreshToken token = new RefreshToken();
        token.setToken("r_token_123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded_new");
        when(refreshTokenRepository.findByUserOrderByExpiryDateAsc(sampleUser)).thenReturn(List.of(token));

        userService.changePassword(1L, req);

        verify(userRepository, times(1)).save(sampleUser);
        verify(redisTemplate, times(1)).delete("refresh:1:r_token_123");
        verify(refreshTokenRepository, times(1)).deleteByUser(sampleUser);
        verify(logEventPublisher, times(1)).publish(any(LogEvent.class));
    }

    @Test
    void changePassword_WrongProvider() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        sampleUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void changePassword_WrongCurrentPassword() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void getAllUsers_ByNumericPrefix() {
        when(userRepository.findById(25L)).thenReturn(Optional.of(sampleUser));

        Page<UserProfileResponse> result = userService.getAllUsers("USR-00025", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("test@ex.com", result.getContent().get(0).getEmail());
    }

    @Test
    void getAllUsers_FuzzySearchActive() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.searchUsersByStatus("Test", true, PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("Test", "ACTIVE", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }
    
    @Test
    void toggleUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        
        userService.toggleUserStatus(1L, false);
        
        assertFalse(sampleUser.getIsActive());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void toggleUserStatus_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.toggleUserStatus(1L, false));
    }

    @Test
    void updateProfile_NotFound() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Updated Name");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(1L, req));
    }

    @Test
    void changePassword_UserNotFound() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.changePassword(1L, req));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        UserProfileResponse response = userService.getUserById(1L);
        assertNotNull(response);
        assertEquals("test@ex.com", response.getEmail());
    }

    @Test
    void getUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getAllUsers_NoSearchNoStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers(null, null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_EmptySearchNoStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_WhitespaceSearchNoStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("   ", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_NoSearchActiveStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findByIsActive(true, PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers(null, "ACTIVE", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_NoSearchSuspendedStatus() {
        sampleUser.setIsActive(false);
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findByIsActive(false, PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers(null, "SUSPENDED", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).getIsActive());
    }

    @Test
    void getAllUsers_FuzzySearchNoStatus() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.searchUsers("Test", PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("Test", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_FuzzySearchSuspendedStatus() {
        sampleUser.setIsActive(false);
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.searchUsersByStatus("Test", false, PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("Test", "SUSPENDED", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_NumericIdWithoutPrefix() {
        when(userRepository.findById(25L)).thenReturn(Optional.of(sampleUser));

        Page<UserProfileResponse> result = userService.getAllUsers("00025", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllUsers_NumericIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Page<UserProfileResponse> result = userService.getAllUsers("USR-00999", null, PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllUsers_NumericIdNotFoundWithStatusFilter() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Page<UserProfileResponse> result = userService.getAllUsers("USR-00999", "ACTIVE", PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllUsers_NumericIdFoundButStatusMismatch() {
        sampleUser.setIsActive(false);
        when(userRepository.findById(25L)).thenReturn(Optional.of(sampleUser));

        Page<UserProfileResponse> result = userService.getAllUsers("USR-00025", "ACTIVE", PageRequest.of(0, 10));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllUsers_InvalidNumericFormat() {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.searchUsers("USR-ABC", PageRequest.of(0, 10))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers("USR-ABC", null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateProfile_NoFieldsChanged() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Test User"); // Same as current

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse resp = userService.updateProfile(1L, req);

        assertEquals("Test User", resp.getFullName());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void changePassword_NoRefreshTokens() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded_new");
        when(refreshTokenRepository.findByUserOrderByExpiryDateAsc(sampleUser)).thenReturn(List.of());

        userService.changePassword(1L, req);

        verify(userRepository, times(1)).save(sampleUser);
        verify(refreshTokenRepository, times(1)).deleteByUser(sampleUser);
    }

    @Test
    void changePassword_MultipleRefreshTokens() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass");
        req.setNewPassword("newPass");

        RefreshToken token1 = new RefreshToken();
        token1.setToken("r_token_1");
        RefreshToken token2 = new RefreshToken();
        token2.setToken("r_token_2");
        RefreshToken token3 = new RefreshToken();
        token3.setToken("r_token_3");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPass", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded_new");
        when(refreshTokenRepository.findByUserOrderByExpiryDateAsc(sampleUser))
                .thenReturn(List.of(token1, token2, token3));

        userService.changePassword(1L, req);

        verify(redisTemplate, times(1)).delete("refresh:1:r_token_1");
        verify(redisTemplate, times(1)).delete("refresh:1:r_token_2");
        verify(redisTemplate, times(1)).delete("refresh:1:r_token_3");
        verify(refreshTokenRepository, times(1)).deleteByUser(sampleUser);
    }
}
