package com.streamhub.report;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.playlist.repository.PlaylistRepository;
import com.streamhub.report.dto.ReportActionRequest;
import com.streamhub.report.dto.ReportResponse;
import com.streamhub.report.dto.ReportReverseActionRequest;
import com.streamhub.report.entity.Report;
import com.streamhub.report.repository.ReportRepository;
import com.streamhub.report.service.ReportService;
import com.streamhub.social.repository.CommentRepository;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test verifying reversal of moderation actions on content reports
public class ReportReverseActionTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PlaylistRepository playlistRepository;

    private ReportService reportService;

    private User moderator;
    private User videoOwner;
    private Video video;
    private Report report;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                userRepository,
                videoRepository,
                commentRepository,
                playlistRepository,
                null,
                null,
                null
        );

        moderator = new User();
        moderator.setUserId(UUID.randomUUID());
        moderator.setUsername("mod1");

        videoOwner = new User();
        videoOwner.setUserId(UUID.randomUUID());
        videoOwner.setUsername("uploader1");

        video = new Video();
        video.setVideoId(UUID.randomUUID());
        video.setTitle("Sample Video");
        video.setOwner(videoOwner);
        video.setHidden(false);

        report = new Report();
        report.setId(UUID.randomUUID());
        report.setReportableType("VIDEO");
        report.setReportableId(video.getVideoId());
        report.setReporter(new User());
        report.setStatus("PENDING");
    }

    @Test
    void testTakeActionAndReverseAction() {
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(userRepository.findById(moderator.getUserId())).thenReturn(Optional.of(moderator));
        when(videoRepository.findById(video.getVideoId())).thenReturn(Optional.of(video));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        //take hide action
        ReportActionRequest actionReq = new ReportActionRequest();
        actionReq.setAction("HIDDEN");
        actionReq.setActionReason("Spam content");
        reportService.takeAction(report.getId(), moderator.getUserId(), actionReq);

        assertTrue(video.isHidden(), "Video should be hidden after moderation action");
        assertEquals("RESOLVED", report.getStatus());
        assertEquals("HIDDEN", report.getModeratorAction());

        //reverse action
        ReportReverseActionRequest reverseReq = new ReportReverseActionRequest("Mistakenly flagged");
        ReportResponse response = reportService.reverseAction(report.getId(), moderator.getUserId(), reverseReq);

        assertFalse(video.isHidden(), "Video should be unhidden after reverse action");
        assertEquals("PENDING", report.getStatus(), "Report status should be reset to PENDING");
        assertNull(report.getModeratorAction(), "Moderator action should be cleared");
        assertTrue(report.getActionReason().contains("Mistakenly flagged"));
    }
}
