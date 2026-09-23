package com.cocheraya.service;

import com.cocheraya.dto.AuthResponse;
import com.cocheraya.dto.LoginRequest;
import com.cocheraya.dto.RegisterRequest;
import com.cocheraya.entity.RefreshToken;
import com.cocheraya.entity.User;
import com.cocheraya.exception.DuplicateResourceException;
import com.cocheraya.exception.UnauthorizedOperationException;
import com.cocheraya.repository.RefreshTokenRepository;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private WalletService walletService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    private User driver;
    private RegisterRequest registerRequest;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        driver = new User();
        driver.setId(1L);
        driver.setName("Christian Conductor");
        driver.setEmail("driver@cocheraya.com");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);

        registerRequest = new RegisterRequest();
        registerRequest.setName("Christian Conductor");
        registerRequest.setEmail("driver@cocheraya.com");
        registerRequest.setPassword("SecurePassword123");
        registerRequest.setPhone("+51987654321");
        registerRequest.setRole("DRIVER");

        refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUser(driver);
        refreshToken.setToken("refresh-token-uuid");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
    }

    @Test
    @DisplayName("shouldRegisterUserSuccessfully")
    void shouldRegisterUserSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(driver);
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("jwt-token-test");
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-test");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(response.getEmail()).isEqualTo("driver@cocheraya.com");
        assertThat(response.getRole()).isEqualTo("DRIVER");

        verify(userRepository).save(any(User.class));
        verify(walletService).initializeWallet(any(User.class));
        verify(eventPublisher).publishEvent(any(com.cocheraya.event.UserRegisteredEvent.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("shouldThrowDuplicateResourceExceptionWhenEmailAlreadyExists")
    void shouldThrowDuplicateResourceExceptionWhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(driver));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Ya existe una cuenta con el email");

        verify(userRepository, never()).save(any());
        verify(walletService, never()).initializeWallet(any());
    }

    @Test
    @DisplayName("shouldLoginSuccessfully")
    void shouldLoginSuccessfully() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("driver@cocheraya.com");
        loginRequest.setPassword("SecurePassword123");
        loginRequest.setFcmToken("fcm-token-123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(driver);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("jwt-token-test");
        when(userRepository.save(any(User.class))).thenReturn(driver);
        when(userRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-test");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-uuid");
        assertThat(response.getEmail()).isEqualTo("driver@cocheraya.com");

        verify(userRepository).save(driver);
        assertThat(driver.getFcmToken()).isEqualTo("fcm-token-123");
    }

    @Test
    @DisplayName("shouldRefreshTokenSuccessfully")
    void shouldRefreshTokenSuccessfully() {
        // Arrange
        when(refreshTokenRepository.findByToken("refresh-token-uuid")).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateToken(any(), any(User.class))).thenReturn("new-jwt-token-test");

        // Act
        AuthResponse response = authService.refreshAccessToken("refresh-token-uuid");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-jwt-token-test");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token-uuid");
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenTokenNotFound")
    void shouldThrowUnauthorizedExceptionWhenTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshAccessToken("unknown-token"))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("Refresh token no registrado");
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedExceptionWhenTokenExpired")
    void shouldThrowUnauthorizedExceptionWhenTokenExpired() {
        // Arrange
        refreshToken.setExpiryDate(Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByToken("refresh-token-uuid")).thenReturn(Optional.of(refreshToken));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshAccessToken("refresh-token-uuid"))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("El refresh token ha expirado");

        verify(refreshTokenRepository).delete(refreshToken);
    }
}
