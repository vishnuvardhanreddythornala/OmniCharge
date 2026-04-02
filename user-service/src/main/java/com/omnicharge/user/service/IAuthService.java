package com.omnicharge.user.service;

import com.omnicharge.user.dto.*;

public interface IAuthService {
    
    void register(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
    
    AuthResponse authenticateWithGoogle(GoogleAuthRequest request);
    
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    void logout(String token);
}
