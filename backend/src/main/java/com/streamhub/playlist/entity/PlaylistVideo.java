package com.streamhub.playlist.entity;

import com.streamhub.video.entity.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "playlist_video", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_playlist_video_Position", columnNames = {"playlist_id", "position"})
})
//database join entity mapping videos into ordered playlist positions
public class PlaylistVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "playlist_video_id", updatable = false, nullable = false)
    private UUID playlistVideoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "position", nullable = false)
    private int position;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public PlaylistVideo() {}

    public PlaylistVideo(Playlist playlist, Video video, int position) {
        this.playlist = playlist;
        this.video = video;
        this.position = position;
    }

    public UUID getPlaylistVideoId() { return playlistVideoId; }
    public void setPlaylistVideoId(UUID playlistVideoId) { this.playlistVideoId = playlistVideoId; }

    public Playlist getPlaylist() { return playlist; }
    public void setPlaylist(Playlist playlist) { this.playlist = playlist; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
