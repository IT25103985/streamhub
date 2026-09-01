package com.streamhub.video;

import com.streamhub.common.entity.User;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.video.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test suite verifying video privacy access control and streaming protections
public class VideoPrivacyTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private VideoService videoService;

    private User owner;
    private User otherUser;
    private User admin;
    private User moderator;
    private Video publicVideo;
    private Video privateVideo;
    private Video hiddenVideo;

    @BeforeEach
    public void setup() {
        videoService = new VideoService(videoRepository, categoryRepository, "uploads", null, null);

        owner = new User();
        owner.setUserId(UUID.randomUUID());
        owner.setUsername("ownerUser");
        owner.setRole("USER");

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setUsername("viewerUser");
        otherUser.setRole("USER");

        admin = new User();
        admin.setUserId(UUID.randomUUID());
        admin.setUsername("adminUser");
        admin.setRole("ADMINISTRATOR");

        moderator = new User();
        moderator.setUserId(UUID.randomUUID());
        moderator.setUsername("modUser");
        moderator.setRole("CONTENT_MODERATOR");

        publicVideo = new Video("Public Title", "Public Desc", "uploads/public.mp4", "/uploads/thumbnails/pub.jpg", owner, "PUBLIC");
        publicVideo.setVideoId(UUID.randomUUID());

        privateVideo = new Video("Private Title", "Private Desc", "uploads/private.mp4", "/uploads/thumbnails/priv.jpg", owner, "PRIVATE");
        privateVideo.setVideoId(UUID.randomUUID());

        hiddenVideo = new Video("Hidden Title", "Hidden Desc", "uploads/hidden.mp4", "/uploads/thumbnails/hid.jpg", owner, "PUBLIC");
        hiddenVideo.setVideoId(UUID.randomUUID());
        hiddenVideo.setHidden(true);
    }

    @Test
    //verifies public videos are accessible to everyone including guests
    public void testPublicVideoAccess() {
        assertTrue(videoService.canUserAccessVideo(publicVideo, (User) null));
        assertTrue(videoService.canUserAccessVideo(publicVideo, otherUser));
        assertTrue(videoService.canUserAccessVideo(publicVideo, owner));
        assertTrue(videoService.canUserAccessVideo(publicVideo, admin));
        assertTrue(videoService.canUserAccessVideo(publicVideo, moderator));
    }

    @Test
    //verifies private videos are blocked for guests and other users
    public void testPrivateVideoBlockedForUnauthorized() {
        assertFalse(videoService.canUserAccessVideo(privateVideo, (User) null));
        assertFalse(videoService.canUserAccessVideo(privateVideo, otherUser));
    }

    @Test
    //verifies private videos are accessible to owner admin and content moderator
    public void testPrivateVideoAccessibleToOwnerAndStaff() {
        assertTrue(videoService.canUserAccessVideo(privateVideo, owner));
        assertTrue(videoService.canUserAccessVideo(privateVideo, admin));
        assertTrue(videoService.canUserAccessVideo(privateVideo, moderator));
    }

    @Test
    //verifies hidden videos are accessible only to owner admin and moderator
    public void testHiddenVideoAccess() {
        assertFalse(videoService.canUserAccessVideo(hiddenVideo, (User) null));
        assertFalse(videoService.canUserAccessVideo(hiddenVideo, otherUser));
        assertTrue(videoService.canUserAccessVideo(hiddenVideo, owner));
        assertTrue(videoService.canUserAccessVideo(hiddenVideo, admin));
        assertTrue(videoService.canUserAccessVideo(hiddenVideo, moderator));
    }

    @Test
    //verifies get video details throws 404 for unauthorized viewers of private video
    public void testGetVideoDetailsUnauthorizedThrows404() {
        when(videoRepository.findById(privateVideo.getVideoId())).thenReturn(Optional.of(privateVideo));

        ResponseStatusException exGuest = assertThrows(ResponseStatusException.class, () -> {
            videoService.getVideoDetails(privateVideo.getVideoId(), null);
        });
        assertEquals(404, exGuest.getStatusCode().value());

        ResponseStatusException exOther = assertThrows(ResponseStatusException.class, () -> {
            videoService.getVideoDetails(privateVideo.getVideoId(), otherUser);
        });
        assertEquals(404, exOther.getStatusCode().value());
    }

    @Test
    //verifies get video details succeeds for owner and staff on private video
    public void testGetVideoDetailsAuthorizedSucceeds() {
        when(videoRepository.findById(privateVideo.getVideoId())).thenReturn(Optional.of(privateVideo));

        VideoResponseDTO ownerResult = videoService.getVideoDetails(privateVideo.getVideoId(), owner);
        assertNotNull(ownerResult);
        assertEquals("PRIVATE", ownerResult.getPrivacy());

        VideoResponseDTO adminResult = videoService.getVideoDetails(privateVideo.getVideoId(), admin);
        assertNotNull(adminResult);
        assertEquals("PRIVATE", adminResult.getPrivacy());
    }

    @Test
    //verifies load video as resource for streaming throws 404 for unauthorized viewers
    public void testLoadVideoAsResourceUnauthorizedThrows404() {
        when(videoRepository.findById(privateVideo.getVideoId())).thenReturn(Optional.of(privateVideo));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            videoService.loadVideoAsResource(privateVideo.getVideoId(), otherUser);
        });
        assertEquals(404, ex.getStatusCode().value());
    }
}
