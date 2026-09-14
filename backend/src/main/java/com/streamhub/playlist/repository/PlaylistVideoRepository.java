package com.streamhub.playlist.repository;

import com.streamhub.playlist.entity.Playlist;
import com.streamhub.playlist.entity.PlaylistVideo;
import com.streamhub.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
//repository managing playlist video order and memberships
public interface PlaylistVideoRepository extends JpaRepository<PlaylistVideo, UUID> {
    Optional<PlaylistVideo> findFirstByPlaylistAndVideo(Playlist playlist, Video video);
    int countByPlaylist(Playlist playlist);
}
