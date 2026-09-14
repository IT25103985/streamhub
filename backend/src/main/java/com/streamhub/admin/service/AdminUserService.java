package com.streamhub.admin.service;

import com.streamhub.admin.dto.AdminUserDto;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//business logic for managing user roles and active banned statuses
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserRole(UUID userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        //only allow recognized system roles
        if (!newRole.equals("ADMINISTRATOR") && !newRole.equals("CONTENT_MODERATOR") && !newRole.equals("USER")) {
            throw new IllegalArgumentException("Invalid role");
        }

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatus(UUID userId, String newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!newStatus.equals("ACTIVE") && !newStatus.equals("BANNED") && !newStatus.equals("DEACTIVATED")) {
            throw new IllegalArgumentException("Invalid status");
        }

        user.setStatus(newStatus);
        userRepository.save(user);
    }
}
