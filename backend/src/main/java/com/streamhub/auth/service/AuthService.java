package com.streamhub.auth.service;

import com.streamhub.auth.dto.AuthResponse;
import com.streamhub.auth.dto.LoginRequest;
import com.streamhub.auth.dto.RegisterRequest;
import com.streamhub.auth.repository.RefreshTokenRepository;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.RefreshToken;
import com.streamhub.common.entity.User;
import com.streamhub.config.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new IllegalArgumentException("Email is already taken");
        }

        int age = Period.between(request.getDateOfBirth(), LocalDate.now()).getYears();
        if (age < 13) {
            throw new IllegalArgumentException("You must be at least 13 years old to register");
        }

        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase() : "VIEWER";
        
        if (requestedRole.equals("ADMINISTRATOR") || requestedRole.equals("CONTENT_MODERATOR")) {
            throw new IllegalArgumentException("Registration with role " + requestedRole + " is not allowed");
        }
        
        if (!requestedRole.equals("VIEWER") && !requestedRole.equals("CONTENT_CREATOR")) {
            requestedRole = "VIEWER";
        }

        User user = new User(
                null,
                request.getUsername(),
                request.getEmail().toLowerCase(),
                request.getFirstName(),
                request.getLastName(),
                request.getDateOfBirth(),
                passwordEncoder.encode(request.getPassword()),
                requestedRole,
                null,
                null
        );

        user = userRepository.save(user);

        // Don't auto-login on register for now, return simple auth response
        return new AuthResponse(
                null, // No token on register
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateToken(user);
        
        String plainRefreshToken = generateSecureToken();
        String hashedToken = hashToken(plainRefreshToken);

        LocalDateTime expiresAt;
        if (request.isKeepMeLoggedIn()) {
            expiresAt = LocalDateTime.now().plusDays(30);
        } else {
            expiresAt = LocalDateTime.now().plusHours(24); // Server backstop
        }

        RefreshToken refreshToken = new RefreshToken(user, hashedToken, expiresAt);
        refreshTokenRepository.save(refreshToken);

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );

        return new LoginResult(authResponse, plainRefreshToken, expiresAt, request.isKeepMeLoggedIn());
    }

    @Transactional
    public LoginResult refresh(String plainRefreshToken) {
        String hashedToken = hashToken(plainRefreshToken);
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateToken(user);

        // Optionally, do rotation: revoke current and issue a new one. 
        // For now we just return a new access token and keep the old refresh token valid.
        
        AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );

        return new LoginResult(authResponse, plainRefreshToken, refreshToken.getExpiresAt(), true);
    }

    @Transactional
    public void logout(String plainRefreshToken) {
        if (plainRefreshToken == null || plainRefreshToken.isBlank()) return;
        String hashedToken = hashToken(plainRefreshToken);
        refreshTokenRepository.findByTokenHash(hashedToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    public static class LoginResult {
        public AuthResponse authResponse;
        public String plainRefreshToken;
        public LocalDateTime expiresAt;
        public boolean isPersistent;

        public LoginResult(AuthResponse authResponse, String plainRefreshToken, LocalDateTime expiresAt, boolean isPersistent) {
            this.authResponse = authResponse;
            this.plainRefreshToken = plainRefreshToken;
            this.expiresAt = expiresAt;
            this.isPersistent = isPersistent;
        }
    }
}
