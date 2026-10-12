package com.streamhub.report.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.report.dto.*;
import com.streamhub.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
//endpoints for submitting content reports and moderator resolution
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    //report submission endpoint
    //submits new report against video comment or user
    @PostMapping
    public ResponseEntity<?> createReport(@Valid @RequestBody ReportCreateRequest request, Authentication authentication) {
        try {
            User user = getUser(authentication);
            ReportResponse response = reportService.createReport(user.getUserId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            if (e.getMessage().startsWith("Conflict")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            } else if (e.getMessage().equals("Reportable content not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    //retrieves paginated reports submitted by authenticated user
    @GetMapping("/mine")
    public ResponseEntity<Page<ReportResponse>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUser(authentication);
        return ResponseEntity.ok(reportService.getMyReports(user.getUserId(), PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))));
    }

    //moderation report list endpoint
    //retrieves paginated reports for moderation dashboard
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CONTENT_MODERATOR')")
    public ResponseEntity<Page<ReportResponse>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reportService.getReports(status, type, PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))));
    }

    //fetches detailed report information including target entity data
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CONTENT_MODERATOR')")
    public ResponseEntity<ReportDetailResponse> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(reportService.getReportDetails(id));
    }

    //updates moderation report status to under review or dismissed
    @RequestMapping(value = {"/{id}/status", "/{id}"}, method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CONTENT_MODERATOR')")
    public ResponseEntity<ReportResponse> updateStatus(@PathVariable UUID id, @Valid @RequestBody ReportStatusRequest request) {
        return ResponseEntity.ok(reportService.updateReportStatus(id, request.getStatus()));
    }

    //executes moderator action such as hide delete or warn
    @PostMapping("/{id}/action")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CONTENT_MODERATOR')")
    public ResponseEntity<?> takeAction(@PathVariable UUID id, @Valid @RequestBody ReportActionRequest request, Authentication authentication) {
        try {
            User moderator = getUser(authentication);
            ReportResponse response = reportService.takeAction(id, moderator.getUserId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('CONTENT_MODERATOR')")
    //reverses previously resolved moderation action on content report
    public ResponseEntity<?> reverseAction(@PathVariable UUID id, @RequestBody(required = false) ReportReverseActionRequest request, Authentication authentication) {
        try {
            User moderator = getUser(authentication);
            ReportResponse response = reportService.reverseAction(id, moderator.getUserId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found")));
    }
}
