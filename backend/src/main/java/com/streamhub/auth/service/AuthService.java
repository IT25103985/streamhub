package com.streamhub.auth.service;

import com.streamhub.auth.dto.AuthResponse;
import com.streamhub.auth.dto.LoginRequest;
import com.streamhub.auth.dto.RegisterRequest;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.config.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already taken");
        }

        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase() : "VIEWER";
        
        // Reject restricted roles
        if (requestedRole.equals("ADMINISTRATOR") || requestedRole.equals("CONTENT_MODERATOR")) {
            throw new IllegalArgumentException("Registration with role " + requestedRole + " is not allowed");
        }
        
        // Validate it's either VIEWER or CONTENT_CREATOR
        if (!requestedRole.equals("VIEWER") && !requestedRole.equals("CONTENT_CREATOR")) {
            requestedRole = "VIEWER"; // fallback or you could throw exception
        }

        User user = new User(
                null,
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                requestedRole,
                null,
                null
        );

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole()
        );
    }
}
