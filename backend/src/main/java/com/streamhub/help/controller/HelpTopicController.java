package com.streamhub.help.controller;

import com.streamhub.help.dto.HelpTopicRequest;
import com.streamhub.help.dto.HelpTopicResponse;
import com.streamhub.help.service.HelpTopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/help/topics", "/api/help/topics"})
//rest controller exposing public and admin endpoints for help topics
public class HelpTopicController {

    private final HelpTopicService helpTopicService;

    public HelpTopicController(HelpTopicService helpTopicService) {
        this.helpTopicService = helpTopicService;
    }

    @GetMapping
    public ResponseEntity<List<HelpTopicResponse>> getAllTopics() {
        return ResponseEntity.ok(helpTopicService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<HelpTopicResponse> createTopic(@Valid @RequestBody HelpTopicRequest request) {
        HelpTopicResponse created = helpTopicService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<HelpTopicResponse> updateTopic(@PathVariable UUID id, @Valid @RequestBody HelpTopicRequest request) {
        HelpTopicResponse updated = helpTopicService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_MODERATOR','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        helpTopicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
