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
//authentication endpoints for register login refresh and logout
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //auth registration endpoint block
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    //auth login endpoint block
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        
        long maxAge = result.isPersistent 
                ? result.expiresAt.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                : -1; //negative value creates a session cookie

        String safeToken = result.plainRefreshToken != null ? result.plainRefreshToken : "";
        ResponseCookie cookie = ResponseCookie.from("refresh_token", safeToken)
                .httpOnly(true)
                .secure(true) //mark cookie as secure
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.authResponse);
    }

    //auth token refresh endpoint block
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }

        try {
            AuthService.LoginResult result = authService.refresh(refreshToken);
            
            //keep existing cookie when refreshing access token
            return ResponseEntity.ok(result.authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        }
    }

    //auth logout endpoint block
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
                .maxAge(0) //expire cookie immediately
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}
