package com.cocheraya.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cocheraya.dto.AuthResponse;
import com.cocheraya.dto.LoginRequest;
import com.cocheraya.dto.RegisterRequest;
import com.cocheraya.dto.TokenRefreshRequest;
import com.cocheraya.entity.User;
import com.cocheraya.exception.DuplicateResourceException;
import com.cocheraya.security.JwtAuthFilter;
import com.cocheraya.security.JwtService;
import com.cocheraya.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Deshabilitar seguridad para pruebas del controller
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAuthService authService;

    // Mock beans para filtros de seguridad cargados por el contexto de Spring WebMvc
    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    private User user;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Christian Conductor");
        user.setEmail("driver@cocheraya.com");
        user.setRole(User.Role.DRIVER);

        authResponse = new AuthResponse("jwt.token.here", "refresh-token-uuid", user);
    }

    @Test
    @DisplayName("shouldRegisterUserSuccessfully")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Christian Conductor");
        request.setEmail("driver@cocheraya.com");
        request.setPassword("SecurePassword123");
        request.setPhone("+51987654321");
        request.setRole("DRIVER");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("driver@cocheraya.com"));
    }

    @Test
    @DisplayName("shouldReturn409WhenEmailAlreadyExists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Christian Conductor");
        request.setEmail("driver@cocheraya.com");
        request.setPassword("SecurePassword123");
        request.setPhone("+51987654321");
        request.setRole("DRIVER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Ya existe una cuenta con el email"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("shouldLoginSuccessfully")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("driver@cocheraya.com");
        request.setPassword("SecurePassword123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    @DisplayName("shouldRefreshTokenSuccessfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("refresh-token-uuid");

        when(authService.refreshAccessToken(anyString())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-uuid"));
    }
}
