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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void getAllUsers_EmptyOrNoSearchNoStatus(String search) {
        Page<User> page = new PageImpl<>(List.of(sampleUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<UserProfileResponse> result = userService.getAllUsers(search, null, PageRequest.of(0, 10));

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

}
