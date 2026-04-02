package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setMobileNumber("9876543210");
        testUser.setRole(Role.ROLE_USER);
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setIsActive(true);
        testUser.setPassword("encodedPassword");
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getProfile_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(1L));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setMobileNumber("1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByMobileNumber("1234567890")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserProfileResponse response = userService.updateProfile(1L, request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        // Note: The mock of save returns testUser which gets its fields mutating during method
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateProfile_DuplicateMobile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");
        request.setMobileNumber("1234567890");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByMobileNumber("1234567890")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.updateProfile(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        userService.changePassword(1L, request);

        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void changePassword_WrongProvider() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest request = new ChangePasswordRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_IncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserProfileResponse> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void toggleUserStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.toggleUserStatus(1L, false);

        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
    }
}
