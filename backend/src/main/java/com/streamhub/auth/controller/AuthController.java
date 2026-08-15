package com.streamhub.auth.controller;

import com.streamhub.auth.dto.AuthResponse;
import com.streamhub.auth.dto.LoginRequest;
import com.streamhub.auth.dto.RegisterRequest;
import com.streamhub.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        
        long maxAge = result.isPersistent 
                ? result.expiresAt.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                : -1; // -1 means session cookie

        String safeToken = result.plainRefreshToken != null ? result.plainRefreshToken : "";
        ResponseCookie cookie = ResponseCookie.from("refresh_token", safeToken)
                .httpOnly(true)
                .secure(true) // Should ideally only be true on HTTPS, but assuming it based on requirements
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        try {
            AuthService.LoginResult result = authService.refresh(refreshToken);
            
            // Optionally update the cookie if you implement token rotation. We just keep the existing one.
            return ResponseEntity.ok(result.authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0) // Clear cookie
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
