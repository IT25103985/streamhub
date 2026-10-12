package com.streamhub.admin;

import com.streamhub.admin.service.AdminUserService;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.playlist.entity.Playlist;
import com.streamhub.playlist.repository.PlaylistRepository;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.video.service.VideoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserBanTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoService videoService;

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query mockQuery;

    private AdminUserService adminUserService;

    private User targetUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository,
                videoRepository,
                videoService,
                playlistRepository,
                entityManager,
                "target/test-avatars",
                "target/test-covers",
                "target/test-playlist-covers"
        );

        userId = UUID.randomUUID();
        targetUser = new User();
        targetUser.setUserId(userId);
        targetUser.setUsername("baduser");
        targetUser.setEmail("baduser@example.com");
        targetUser.setFirstName("Bad");
        targetUser.setLastName("Actor");
        targetUser.setRole("USER");
        targetUser.setStatus("ACTIVE");
        targetUser.setBio("Spam bio");
        targetUser.setAvatarUrl("/api/v1/users/avatar/bad_avatar.png");
        targetUser.setCoverImageUrl("/api/v1/users/cover/bad_cover.png");
    }

    @Test
    void testBanUser_purgesVideosPlaylistsAndContent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

        Video video1 = new Video();
        video1.setVideoId(UUID.randomUUID());
        video1.setTitle("Spam Video 1");
        video1.setOwner(targetUser);

        when(videoRepository.findAllByOwner_UserId(userId)).thenReturn(List.of(video1));

        Playlist playlist1 = new Playlist("Spam Playlist", targetUser, "PUBLIC", "desc", "/uploads/playlist-covers/cover1.jpg");
        when(playlistRepository.findByOwnerOrderByCreatedAtDesc(targetUser)).thenReturn(List.of(playlist1));

        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

        // Act: admin bans the user
        adminUserService.updateUserStatus(userId, "BANNED");

        // Assert: user status is BANNED
        assertEquals("BANNED", targetUser.getStatus());
        assertNull(targetUser.getBio());
        assertNull(targetUser.getAvatarUrl());
        assertNull(targetUser.getCoverImageUrl());

        // Assert: video deletion service called for all user's videos
        verify(videoService, times(1)).deleteVideoInternal(eq(video1), eq("Uploader account banned"), isNull());

        // Assert: playlist deleted
        verify(playlistRepository, times(1)).delete(playlist1);

        // Assert: dependent relations deleted via native queries
        verify(entityManager, atLeast(5)).createNativeQuery(anyString());
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    void testActivateUser_doesNotPurgeContent() {
        targetUser.setStatus("DEACTIVATED");
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));

        adminUserService.updateUserStatus(userId, "ACTIVE");

        assertEquals("ACTIVE", targetUser.getStatus());
        verify(videoService, never()).deleteVideoInternal(any(), any(), any());
        verify(playlistRepository, never()).delete(any());
        verify(userRepository, times(1)).save(targetUser);
    }

    @Test
    void testDeleteUser_purgesAllDataAndRemovesUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(videoRepository.findAllByOwner_UserId(userId)).thenReturn(Collections.emptyList());
        when(playlistRepository.findByOwnerOrderByCreatedAtDesc(targetUser)).thenReturn(Collections.emptyList());
        when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
        when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

        adminUserService.deleteUser(userId);

        verify(userRepository, times(1)).delete(targetUser);
    }
}
