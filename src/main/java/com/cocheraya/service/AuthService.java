package com.cocheraya.service;

import com.cocheraya.dto.AuthResponse;
import com.cocheraya.dto.LoginRequest;
import com.cocheraya.dto.RegisterRequest;
import com.cocheraya.entity.RefreshToken;
import com.cocheraya.entity.User;
import com.cocheraya.exception.DuplicateResourceException;
import com.cocheraya.repository.RefreshTokenRepository;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;

    
    public AuthResponse register(RegisterRequest request) {
        
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        
        walletService.initializeWallet(savedUser);

        
        String jwt = jwtService.generateToken(
                Map.of("role", savedUser.getRole().name()), savedUser
        );

        
        eventPublisher.publishEvent(new com.cocheraya.event.UserRegisteredEvent(
                this, savedUser.getId(), savedUser.getEmail(), savedUser.getName(), savedUser.getRole()
        ));

        
        RefreshToken refreshToken = createRefreshToken(savedUser.getId());

        log.info("Usuario registrado exitosamente: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return new AuthResponse(jwt, refreshToken.getToken(), savedUser);
    }

    
    public AuthResponse login(LoginRequest request) {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        
        if (request.getFcmToken() != null && !request.getFcmToken().isBlank()) {
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);
            log.info("FCM Token actualizado para usuario id={}", user.getId());
        }

        
        String jwt = jwtService.generateToken(
                Map.of("role", user.getRole().name()), user
        );

        
        RefreshToken refreshToken = createRefreshToken(user.getId());

        log.info("Usuario autenticado exitosamente: id={}, email={}", user.getId(), user.getEmail());
        return new AuthResponse(jwt, refreshToken.getToken(), user);
    }

    /**
     * Crea o actualiza el Refresh Token para un usuario.
     * Expiración establecida en 7 días (604,800,000 ms).
     */
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));

        
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000));
        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    
    public AuthResponse refreshAccessToken(String requestRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new com.cocheraya.exception.UnauthorizedOperationException(
                        "Refresh token no registrado en el sistema."));

        // Verificar expiración
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new com.cocheraya.exception.UnauthorizedOperationException(
                    "El refresh token ha expirado. Por favor inicie sesión nuevamente.");
        }

        User user = token.getUser();
        String jwt = jwtService.generateToken(
                Map.of("role", user.getRole().name()), user
        );

        log.info("JWT renovado exitosamente para usuario: {}", user.getEmail());
        return new AuthResponse(jwt, token.getToken(), user);
    }
}
