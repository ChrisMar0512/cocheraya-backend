package com.cocheraya.service;

import com.cocheraya.dto.AuthResponse;
import com.cocheraya.dto.LoginRequest;
import com.cocheraya.dto.RegisterRequest;
import com.cocheraya.entity.RefreshToken;

public interface IAuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    RefreshToken createRefreshToken(Long userId);
    AuthResponse refreshAccessToken(String requestRefreshToken);
}
