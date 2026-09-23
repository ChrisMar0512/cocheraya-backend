package com.cocheraya.dto;

import com.cocheraya.entity.User;
import lombok.Data;

@Data
public class AuthResponse {

    
    private String token;

    private String refreshToken;

    private Long userId;
    private String name;
    private String email;
    private String role;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
    }

    public AuthResponse(String token, String refreshToken, User user) {
        this(token, user);
        this.refreshToken = refreshToken;
    }
}
