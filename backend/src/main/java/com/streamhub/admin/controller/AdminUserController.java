package com.streamhub.admin.controller;

import com.streamhub.admin.dto.AdminUserDto;
import com.streamhub.admin.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
//admin endpoints for user role and account status management
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    //returns all registered user accounts with roles and statuses
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    @PatchMapping("/{id}/role")
    //changes role to administrator moderator or standard user
    public ResponseEntity<Void> updateUserRole(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (role == null) {
            return ResponseEntity.badRequest().build();
        }
        adminUserService.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/status")
    //updates account status to active banned or deactivated
    public ResponseEntity<Void> updateUserStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().build();
        }
        adminUserService.updateUserStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
