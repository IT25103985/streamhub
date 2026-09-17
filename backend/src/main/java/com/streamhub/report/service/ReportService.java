package com.streamhub.report.service;

import com.streamhub.common.entity.User;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.notification.service.NotificationService;
import com.streamhub.playlist.entity.Playlist;
import com.streamhub.playlist.repository.PlaylistRepository;
import com.streamhub.report.dto.*;
import com.streamhub.report.entity.Report;
import com.streamhub.report.repository.ReportRepository;
import com.streamhub.social.entity.Comment;
import com.streamhub.social.repository.CommentRepository;
import com.streamhub.social.service.SocialService;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.video.service.VideoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@SuppressWarnings("null")
//service handling report creation validation and moderation actions
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;
    private final PlaylistRepository playlistRepository;
    private final VideoService videoService;
    private final SocialService socialService;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
                         VideoRepository videoRepository, CommentRepository commentRepository,
                         PlaylistRepository playlistRepository, VideoService videoService,
                         SocialService socialService,
                         NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.commentRepository = commentRepository;
        this.playlistRepository = playlistRepository;
        this.videoService = videoService;
        this.socialService = socialService;
        this.notificationService = notificationService;
    }

    @Transactional
    //report creation and validation logic
    public ReportResponse createReport(UUID reporterId, ReportCreateRequest request) {
        if (reportRepository.existsByReporter_UserIdAndReportableTypeAndReportableIdAndStatus(
                reporterId, request.getReportableType(), request.getReportableId(), "PENDING")) {
            throw new RuntimeException("Conflict: You already have a pending report for this content");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        validateReportableContent(request.getReportableType(), request.getReportableId(), reporterId);

        Report report = new Report(reporter, request.getReportableType(), request.getReportableId(),
                request.getReason(), request.getDetails());

        Report saved = reportRepository.save(report);
        String previewText = getPreviewText(saved);

        if (notificationService != null && reporter != null) {
            try {
                notificationService.createNotification(
                    reporter.getUserId(),
                    "REPORT_SUBMITTED",
                    "Report Submitted",
                    "Your report regarding " + request.getReportableType().toLowerCase() + " (\"" + previewText + "\") has been submitted and is pending review by our moderation team.",
                    "REPORT",
                    saved.getId()
                );
            } catch (Exception e) {
                System.err.println("Failed to send report submission notification: " + e.getMessage());
            }
        }

        return new ReportResponse(saved, previewText);
    }
    //READ
    @Transactional(readOnly = true)
    public Page<ReportResponse> getMyReports(UUID reporterId, Pageable pageable) {
        return reportRepository.findByReporter_UserId(reporterId, pageable)
                .map(r -> new ReportResponse(r, getPreviewText(r)));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getReports(String status, String type, Pageable pageable) {
        Page<Report> reports;
        if (status != null && type != null) {
            reports = reportRepository.findByStatusAndReportableType(status, type, pageable);
        } else if (status != null) {
            reports = reportRepository.findByStatus(status, pageable);
        } else if (type != null) {
            reports = reportRepository.findByReportableType(type, pageable);
        } else {
            reports = reportRepository.findAll(pageable);
        }
        return reports.map(r -> new ReportResponse(r, getPreviewText(r)));
    }

    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetails(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        return new ReportDetailResponse(new ReportResponse(report, getPreviewText(report)), buildContentSnapshot(report));
    }
//Update
    @Transactional
    public ReportResponse updateReportStatus(UUID reportId, String newStatus) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        //status transition validation placeholder
        report.setStatus(newStatus);
        if ("RESOLVED".equals(newStatus)) {
            report.setResolvedAt(LocalDateTime.now());
        }
        Report savedReport = reportRepository.save(report);
        String previewText = getPreviewText(savedReport);

        if (notificationService != null && report.getReporter() != null) {
            try {
                notificationService.createNotification(
                    report.getReporter().getUserId(),
                    "REPORT_STATUS_UPDATE",
                    "Report Status: " + newStatus,
                    "Your report regarding " + report.getReportableType().toLowerCase() + " (\"" + previewText + "\") status has been updated to " + newStatus + ".",
                    "REPORT",
                    report.getId()
                );
            } catch (Exception e) {
                System.err.println("Failed to send report status update notification: " + e.getMessage());
            }
        }

        return new ReportResponse(savedReport, previewText);
    }

    @Transactional
    //moderation action execution logic
    public ReportResponse takeAction(UUID reportId, UUID moderatorId, ReportActionRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        String type = report.getReportableType();
        String action = request.getAction();
        UUID ownerId = null;
// Moderation Business Rule Validation
        if ("PROFILE".equals(type) && ("HIDDEN".equals(action) || "DELETED".equals(action))) {
            throw new IllegalArgumentException("Cannot hide or delete a user profile directly");
        }

        switch (type) {
            case "VIDEO":
                Video video = videoRepository.findById(report.getReportableId()).orElse(null);
                if (video != null) {
                    ownerId = video.getOwner().getUserId();
                    if ("HIDDEN".equals(action)) {
                        video.setHidden(true);
                        video.setPrivacy("PRIVATE");
                        videoRepository.save(video);

                        
                        //DELETE Video----------------
                        
                    } else if ("DELETED".equals(action)) {
                        videoService.moderateDeleteVideo(video.getVideoId(), request.getActionReason(), moderator);
                    }
                }
                break;
            case "COMMENT":
                Comment comment = commentRepository.findById(report.getReportableId()).orElse(null);
                if (comment != null) {
                    ownerId = comment.getAuthor().getUserId();
                    if ("HIDDEN".equals(action)) {
                        comment.setHidden(true);
                        commentRepository.save(comment);
                        //COMMENT delete------------------
                    } else if ("DELETED".equals(action)) {
                        socialService.moderateDeleteComment(comment.getCommentId());
                    }
                }
                break;
            case "PLAYLIST":
                Playlist playlist = playlistRepository.findById(report.getReportableId()).orElse(null);
                if (playlist != null) {
                    ownerId = playlist.getOwner().getUserId();
                    if ("HIDDEN".equals(action)) {
                        playlist.setHidden(true);
                        playlistRepository.save(playlist);
                        //Playlist deleted------------------------
                    } else if ("DELETED".equals(action)) {
                        //remove reported playlist entity
                        playlistRepository.delete(playlist);
                    }
                }
                break;
            case "PROFILE":
                User reportedUser = userRepository.findById(report.getReportableId()).orElse(null);
                if (reportedUser != null) {
                    ownerId = reportedUser.getUserId();
                }
                break;
        }

        report.setStatus("RESOLVED");
        report.setModerator(moderator);
        report.setModeratorAction(action);
        report.setActionReason(request.getActionReason());
        report.setResolvedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        String previewText = getPreviewText(savedReport);

        // Notify reporter that action was taken on their submitted report
        if (notificationService != null && report.getReporter() != null) {
            try {
                String actionNote = (request.getActionReason() != null && !request.getActionReason().trim().isEmpty()) 
                        ? " Reason/Note: " + request.getActionReason().trim() : "";
                notificationService.createNotification(
                    report.getReporter().getUserId(),
                    "REPORT_RESOLVED",
                    "Report Status Update",
                    "Your report regarding " + type.toLowerCase() + " (\"" + previewText + "\") has been reviewed by a moderator. Action taken: " + action + "." + actionNote,
                    "REPORT",
                    report.getId()
                );
            } catch (Exception e) {
                System.err.println("Failed to send report action notification to reporter: " + e.getMessage());
            }
        }

        // Notify content owner if moderation action was taken
        if (ownerId != null && !"NONE".equals(action) && notificationService != null) {
            String title = "Moderation Action Taken on Your " + type.charAt(0) + type.substring(1).toLowerCase();
            String reasonText = (request.getActionReason() != null && !request.getActionReason().trim().isEmpty())
                    ? request.getActionReason().trim()
                    : "Violation of platform community guidelines";
            try {
                notificationService.createNotification(
                    ownerId,
                    "MODERATION_ACTION",
                    title,
                    "A moderation action (" + action + ") was taken on your " + type.toLowerCase() + " (\"" + previewText + "\"). Reason: " + reasonText + ".",
                    type,
                    report.getReportableId()
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to send moderation notification to owner: " + notifEx.getMessage());
            }
        }

        return new ReportResponse(savedReport, previewText);
    }

    @Transactional
    //reverses moderation action taken on report restoring content visibility
    public ReportResponse reverseAction(UUID reportId, UUID moderatorId, ReportReverseActionRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new RuntimeException("Moderator not found"));

        String type = report.getReportableType();
        String prevAction = report.getModeratorAction();
        UUID targetOwnerId = null;

        //unhide previously hidden content
        if ("HIDDEN".equals(prevAction)) {
            switch (type) {
                case "VIDEO":
                    Video video = videoRepository.findById(report.getReportableId()).orElse(null);
                    if (video != null) {
                        targetOwnerId = video.getOwner().getUserId();
                        video.setHidden(false);
                        video.setPrivacy("PUBLIC");
                        videoRepository.save(video);
                    }
                    break;
                case "COMMENT":
                    Comment comment = commentRepository.findById(report.getReportableId()).orElse(null);
                    if (comment != null) {
                        targetOwnerId = comment.getAuthor().getUserId();
                        comment.setHidden(false);
                        commentRepository.save(comment);
                    }
                    break;
                case "PLAYLIST":
                    Playlist playlist = playlistRepository.findById(report.getReportableId()).orElse(null);
                    if (playlist != null) {
                        targetOwnerId = playlist.getOwner().getUserId();
                        playlist.setHidden(false);
                        playlistRepository.save(playlist);
                    }
                    break;
                case "PROFILE":
                    User reportedUser = userRepository.findById(report.getReportableId()).orElse(null);
                    if (reportedUser != null) {
                        targetOwnerId = reportedUser.getUserId();
                    }
                    break;
            }
        } else {
            //identify owner for notification on other reversed actions
            switch (type) {
                case "VIDEO":
                    Video v = videoRepository.findById(report.getReportableId()).orElse(null);
                    if (v != null) targetOwnerId = v.getOwner().getUserId();
                    break;
                case "COMMENT":
                    Comment c = commentRepository.findById(report.getReportableId()).orElse(null);
                    if (c != null) targetOwnerId = c.getAuthor().getUserId();
                    break;
                case "PLAYLIST":
                    Playlist p = playlistRepository.findById(report.getReportableId()).orElse(null);
                    if (p != null) targetOwnerId = p.getOwner().getUserId();
                    break;
                case "PROFILE":
                    User u = userRepository.findById(report.getReportableId()).orElse(null);
                    if (u != null) targetOwnerId = u.getUserId();
                    break;
            }
        }
// Input Sanitization & Fallback Validation
        String reason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : "Moderation action was reversed after review";

        report.setStatus("PENDING");
        report.setModerator(moderator);
        report.setModeratorAction(null);
        report.setActionReason("Action reversed: " + reason);
        report.setResolvedAt(null);

        if (targetOwnerId != null && notificationService != null) {
            String title = "Moderation action reversed on your " + type.toLowerCase();
            String msg = "The previous moderation action on your " + type.toLowerCase() + " has been reversed. Reason: " + reason;
            try {
                notificationService.createNotification(
                        targetOwnerId,
                        "MODERATION",
                        title,
                        msg,
                        type,
                        report.getReportableId()
                );
            } catch (Exception notifEx) {
                System.err.println("Failed to send reversal notification: " + notifEx.getMessage());
            }
        }

        return new ReportResponse(reportRepository.save(report), getPreviewText(report));
    }
    //Entity Existence & Data Integrity Validation

    private void validateReportableContent(String type, UUID id, UUID reporterId) {
        if (type == null || id == null) {
            throw new IllegalArgumentException("Reportable type and ID must not be null");
        }
        switch (type.toUpperCase()) {
            case "VIDEO": {
                Video video = videoRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Reportable content not found"));
                if (video.getOwner() != null && video.getOwner().getUserId().equals(reporterId)) {
                    throw new IllegalArgumentException("You cannot report your own video");
                }
                break;
            }
            case "COMMENT": {
                Comment comment = commentRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Reportable content not found"));
                if (comment.getAuthor() != null && comment.getAuthor().getUserId().equals(reporterId)) {
                    throw new IllegalArgumentException("You cannot report your own comment");
                }
                break;
            }
            case "PLAYLIST": {
                Playlist playlist = playlistRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Reportable content not found"));
                if (playlist.getOwner() != null && playlist.getOwner().getUserId().equals(reporterId)) {
                    throw new IllegalArgumentException("You cannot report your own playlist");
                }
                break;
            }
            case "PROFILE": {
                User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Reportable content not found"));
                if (user.getUserId().equals(reporterId)) {
                    throw new IllegalArgumentException("You cannot report your own profile");
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid reportable type");
        }
    }

    private String getPreviewText(Report report) {
        try {
            switch (report.getReportableType()) {
                case "VIDEO":
                    return videoRepository.findById(report.getReportableId())
                            .map(Video::getTitle).orElse("[Deleted Video]");
                case "COMMENT":
                    return commentRepository.findById(report.getReportableId())
                            .map(c -> c.getText().length() > 50 ? c.getText().substring(0, 47) + "..." : c.getText())
                            .orElse("[Deleted Comment]");
                case "PLAYLIST":
                    return playlistRepository.findById(report.getReportableId())
                            .map(Playlist::getName).orElse("[Deleted Playlist]");
                case "PROFILE":
                    return userRepository.findById(report.getReportableId())
                            .map(User::getUsername).orElse("[Deleted User]");
            }
        } catch (Exception e) {
            //ignore lookup error and return fallback label
        }
        return "Unknown Content";
    }

    private Map<String, Object> buildContentSnapshot(Report report) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("type", report.getReportableType());
        
        switch (report.getReportableType()) {
            case "VIDEO":
                videoRepository.findById(report.getReportableId()).ifPresent(v -> {
                    snapshot.put("title", v.getTitle());
                    snapshot.put("uploader", v.getOwner().getUsername());
                    snapshot.put("thumbnail", v.getThumbnailUrl()); //store thumbnail path in snapshot
                });
                break;
            case "COMMENT":
                commentRepository.findById(report.getReportableId()).ifPresent(c -> {
                    snapshot.put("text", c.getText());
                    snapshot.put("author", c.getAuthor().getUsername());
                    snapshot.put("videoTitle", c.getVideo().getTitle());
                });
                break;
            case "PLAYLIST":
                playlistRepository.findById(report.getReportableId()).ifPresent(p -> {
                    snapshot.put("name", p.getName());
                    snapshot.put("owner", p.getOwner().getUsername());
                    snapshot.put("videoCount", p.getPlaylistVideos().size());
                });
                break;
            case "PROFILE":
                userRepository.findById(report.getReportableId()).ifPresent(u -> {
                    snapshot.put("username", u.getUsername());
                    snapshot.put("email", u.getEmail());
                    snapshot.put("joinDate", u.getCreatedAt());
                });
                break;
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        if (status == null) {
            return 0L;
        }
        return reportRepository.findAll().stream()
                .filter(r -> r.getStatus() != null && r.getStatus().equalsIgnoreCase(status))
                .count();
    }
}
