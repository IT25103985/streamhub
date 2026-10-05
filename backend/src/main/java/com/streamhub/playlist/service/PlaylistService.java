package com.streamhub.playlist.service;

import com.streamhub.common.entity.User;
import com.streamhub.playlist.dto.CreatePlaylistDTO;
import com.streamhub.playlist.dto.PlaylistDetailDTO;
import com.streamhub.playlist.dto.PlaylistResponseDTO;
import com.streamhub.playlist.dto.UpdatePlaylistDTO;
import com.streamhub.playlist.entity.Playlist;
import com.streamhub.playlist.entity.PlaylistVideo;
import com.streamhub.playlist.event.PlaylistVideoAddedEvent;
import com.streamhub.playlist.repository.PlaylistRepository;
import com.streamhub.playlist.repository.PlaylistVideoRepository;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
//manages playlist creation track sequencing and privacy access rules
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistVideoRepository playlistVideoRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Path coverStorageLocation;

    @org.springframework.beans.factory.annotation.Autowired
    public PlaylistService(PlaylistRepository playlistRepository, 
                           PlaylistVideoRepository playlistVideoRepository, 
                           VideoRepository videoRepository,
                           UserRepository userRepository,
                           ApplicationEventPublisher eventPublisher,
                           @Value("${file.playlist-cover-upload-dir:uploads/playlist-covers}") String coverUploadDir) {
        this.playlistRepository = playlistRepository;
        this.playlistVideoRepository = playlistVideoRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.coverStorageLocation = Paths.get(coverUploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.coverStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create directory for playlist covers.", ex);
        }
    }

    public PlaylistService(PlaylistRepository playlistRepository, 
                           PlaylistVideoRepository playlistVideoRepository, 
                           VideoRepository videoRepository,
                           UserRepository userRepository,
                           @Value("${file.playlist-cover-upload-dir:uploads/playlist-covers}") String coverUploadDir) {
        this(playlistRepository, playlistVideoRepository, videoRepository, userRepository, null, coverUploadDir);
    }

    @Transactional
    //playlist creation business logic
    public PlaylistResponseDTO createPlaylist(CreatePlaylistDTO dto, User owner, MultipartFile thumbnail) {
        String thumbnailUrl = null;
        if (thumbnail != null && !thumbnail.isEmpty()) {
            thumbnailUrl = saveFile(thumbnail);
        }

        Playlist playlist = new Playlist(dto.getName(), owner, dto.getPrivacy(), dto.getDescription(), thumbnailUrl);
        Playlist saved = playlistRepository.save(playlist);
        return mapToResponseDTO(saved);
    }

    //updates existing playlist fields and optionally changes cover thumbnail
    @Transactional
    public PlaylistResponseDTO updatePlaylist(UUID playlistId, UpdatePlaylistDTO dto, User owner, MultipartFile thumbnail) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        
        if (!playlist.getOwner().getUserId().equals(owner.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only edit your own playlists");
        }

        playlist.setName(dto.getName());
        playlist.setPrivacy(dto.getPrivacy());
        playlist.setDescription(dto.getDescription());

        if (dto.isRemoveThumbnail()) {
            playlist.setThumbnailUrl(null);
        } else if (thumbnail != null && !thumbnail.isEmpty()) {
            String newThumbnailUrl = saveFile(thumbnail);
            playlist.setThumbnailUrl(newThumbnailUrl);
        }

        Playlist saved = playlistRepository.save(playlist);
        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PlaylistResponseDTO> getUserPlaylists(User owner, boolean onlyOwnVideos) {
        return getUserPlaylists(owner, onlyOwnVideos, null);
    }

    @Transactional(readOnly = true)
    //fetches user playlists filtered by own videos constraint and search query
    public List<PlaylistResponseDTO> getUserPlaylists(User owner, boolean onlyOwnVideos, String search) {
        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        List<Playlist> playlists = playlistRepository.findUserPlaylists(owner.getUserId(), onlyOwnVideos, sanitizedSearch);
        return playlists.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    //fetches all public non hidden playlists across the platform
    @Transactional(readOnly = true)
    public List<PlaylistResponseDTO> getPublicPlaylists() {
        return playlistRepository.findPublicPlaylists().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    //searches public playlists by query string and category filter
    public org.springframework.data.domain.Page<PlaylistResponseDTO> searchPlaylists(String query, UUID categoryId, org.springframework.data.domain.Pageable pageable) {
        String sanitizedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        org.springframework.data.domain.Page<Playlist> page = playlistRepository.searchPublicPlaylists(sanitizedQuery, categoryId, pageable);
        return page.map(this::mapToResponseDTO);
    }

    //retrieves playlists owned by specific user respecting privacy rules
    @Transactional(readOnly = true)
    public List<PlaylistResponseDTO> getPlaylistsByOwner(UUID ownerId, boolean onlyOwnVideos, User requester) {
        List<Playlist> playlists;
        if (onlyOwnVideos) {
            playlists = playlistRepository.findByOwnerIdAndOnlyOwnVideos(ownerId);
        } else {
            User owner = userRepository.findById(ownerId).orElseThrow(() -> new RuntimeException("User not found"));
            playlists = playlistRepository.findByOwnerOrderByCreatedAtDesc(owner);
        }

        boolean isOwner = (requester != null && requester.getUserId().equals(ownerId));
        boolean isAdminOrMod = requester != null && ("ADMINISTRATOR".equals(requester.getRole()) || "CONTENT_MODERATOR".equals(requester.getRole()));

        return playlists.stream()
                .filter(p -> {
                    if (p.isHidden() && !isOwner && !isAdminOrMod) {
                        return false;
                    }
                    return isOwner || isAdminOrMod || "PUBLIC".equals(p.getPrivacy());
                })
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    //fetches detailed playlist entity with ordered list of accessible videos
    @Transactional(readOnly = true)
    public PlaylistDetailDTO getPlaylistDetails(UUID playlistId, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        
        boolean isOwner = currentUser != null && playlist.getOwner() != null && currentUser.getUserId().equals(playlist.getOwner().getUserId());
        boolean isAdminOrMod = currentUser != null && ("ADMINISTRATOR".equals(currentUser.getRole()) || "CONTENT_MODERATOR".equals(currentUser.getRole()));

        if (playlist.isHidden() && !isOwner && !isAdminOrMod) {
            throw new RuntimeException("Playlist not found");
        }

        if ("PRIVATE".equals(playlist.getPrivacy()) && !isOwner && !isAdminOrMod) {
            throw new RuntimeException("Playlist not found"); //hide private playlist existence from non owners
        }
        
        return mapToDetailDTO(playlist, currentUser);
    }

    @Transactional
    //playlist track append logic
    public void addVideoToPlaylist(UUID playlistId, UUID videoId, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
                
        if (!playlist.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only add videos to your own playlists");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (playlistVideoRepository.findFirstByPlaylistAndVideo(playlist, video).isPresent()) {
            throw new IllegalArgumentException("This video is already in the playlist");
        }

        int position = playlistVideoRepository.countByPlaylist(playlist);
        PlaylistVideo playlistVideo = new PlaylistVideo(playlist, video, position);
        playlistVideoRepository.save(playlistVideo);

        // Publish event to notify video owner if another user adds their video to a public playlist
        if (eventPublisher != null && "PUBLIC".equalsIgnoreCase(playlist.getPrivacy()) 
                && video.getOwner() != null && currentUser != null 
                && !video.getOwner().getUserId().equals(currentUser.getUserId())) {
            eventPublisher.publishEvent(new PlaylistVideoAddedEvent(
                    playlist.getPlaylistId(),
                    playlist.getName(),
                    video.getVideoId(),
                    video.getTitle(),
                    currentUser.getUserId(),
                    currentUser.getUsername(),
                    video.getOwner().getUserId()
            ));
        }
    }

    //removes video from playlist and collapses indices to maintain contiguous order
    @Transactional
    public void removeVideoFromPlaylist(UUID playlistId, UUID videoId, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only remove videos from your own playlists");
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        PlaylistVideo playlistVideo = playlistVideoRepository.findFirstByPlaylistAndVideo(playlist, video)
                .orElseThrow(() -> new RuntimeException("Video is not in the playlist"));

        //remove from parent collection to maintain cascade and orphanRemoval consistency
        playlist.getPlaylistVideos().remove(playlistVideo);
        playlistVideoRepository.delete(playlistVideo);
        playlistVideoRepository.flush();
        
        //reorder remaining positions in two passes to avoid unique key collision
        List<PlaylistVideo> remaining = playlist.getPlaylistVideos().stream()
                .sorted(Comparator.comparingInt(PlaylistVideo::getPosition))
                .collect(Collectors.toList());
                
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(-1 - i);
        }
        playlistVideoRepository.saveAllAndFlush(remaining);

        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        playlistVideoRepository.saveAllAndFlush(remaining);
    }

    @Transactional
    //playlist reordering position sequencing logic
    public void reorderVideos(UUID playlistId, List<UUID> orderedVideoIds, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only reorder your own playlists");
        }

        List<PlaylistVideo> currentVideos = playlist.getPlaylistVideos();
        if (currentVideos.size() != orderedVideoIds.size()) {
            throw new IllegalArgumentException("Mismatched list length for reordering");
        }

        //temporary negative indices prevent unique constraint violations
        for (int i = 0; i < currentVideos.size(); i++) {
            currentVideos.get(i).setPosition(-1 - i);
        }
        playlistVideoRepository.saveAllAndFlush(currentVideos);

        //assign final zero based index positions
        for (int i = 0; i < orderedVideoIds.size(); i++) {
            UUID vidId = orderedVideoIds.get(i);
            PlaylistVideo pvToUpdate = currentVideos.stream()
                    .filter(pv -> pv.getVideo().getVideoId().equals(vidId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Video ID " + vidId + " does not belong to this playlist"));
            pvToUpdate.setPosition(i);
        }
        playlistVideoRepository.saveAllAndFlush(currentVideos);
    }

    //deletes playlist entity owned by current user
    @Transactional
    public void deletePlaylist(UUID playlistId, User currentUser) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only delete your own playlists");
        }

        playlistRepository.delete(playlist);
    }

    //retrieves all platform playlists for administrator review
    @Transactional(readOnly = true)
    public List<PlaylistResponseDTO> getAllPlaylistsForAdmin() {
        return playlistRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    //toggles hidden status of playlist for moderation purposes
    @Transactional
    public void togglePlaylistHidden(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        
        playlist.setHidden(!playlist.isHidden());
        playlistRepository.save(playlist);
    }

    //administratively removes playlist as content moderator
    @Transactional
    public void deletePlaylistAsModerator(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));
        
        playlistRepository.delete(playlist);
    }

    private String getComputedThumbnail(Playlist playlist) {
        if (playlist.getThumbnailUrl() != null && !playlist.getThumbnailUrl().trim().isEmpty()) {
            return playlist.getThumbnailUrl();
        }

        List<PlaylistVideo> videos = playlist.getPlaylistVideos();
        if (videos != null && !videos.isEmpty()) {
            for (PlaylistVideo pv : videos) {
                if (pv.getVideo() != null && pv.getVideo().getThumbnailUrl() != null && !pv.getVideo().getThumbnailUrl().trim().isEmpty()) {
                    return pv.getVideo().getThumbnailUrl();
                }
            }
        }
        return null;
    }

    private PlaylistResponseDTO mapToResponseDTO(Playlist playlist) {
        return new PlaylistResponseDTO(
                playlist.getPlaylistId(),
                playlist.getName(),
                playlist.getOwner().getUsername(),
                playlist.getPrivacy(),
                getComputedThumbnail(playlist),
                playlist.isHidden(),
                playlist.getPlaylistVideos().size(),
                playlist.getCreatedAt()
        );
    }

    private PlaylistDetailDTO mapToDetailDTO(Playlist playlist, User currentUser) {
        List<VideoResponseDTO> videos = playlist.getPlaylistVideos().stream()
                .filter(pv -> {
                    Video v = pv.getVideo();
                    if (v == null) return false;
                    boolean isOwner = currentUser != null && v.getOwner() != null && currentUser.getUserId().equals(v.getOwner().getUserId());
                    boolean isAdminOrMod = currentUser != null && ("ADMINISTRATOR".equals(currentUser.getRole()) || "CONTENT_MODERATOR".equals(currentUser.getRole()));
                    if (v.isHidden() && !isOwner && !isAdminOrMod) return false;
                    if ("PRIVATE".equalsIgnoreCase(v.getPrivacy()) && !isOwner && !isAdminOrMod) return false;
                    return true;
                })
                .map(pv -> {
                    Video v = pv.getVideo();
                    VideoResponseDTO dto = new VideoResponseDTO(
                            v.getVideoId(),
                            v.getTitle(),
                            v.getDescription(),
                            v.getOwner().getUserId(),
                            v.getOwner().getUsername(),
                            v.getOwner().getAvatarUrl(),
                            v.getThumbnailUrl(),
                            v.getCreatedAt(),
                            null
                    );
                    dto.setHidden(v.isHidden());
                    dto.setPrivacy(v.getPrivacy());
                    return dto;
                })
                .collect(Collectors.toList());

        return new PlaylistDetailDTO(
                playlist.getPlaylistId(),
                playlist.getName(),
                playlist.getOwner().getUsername(),
                playlist.getPrivacy(),
                playlist.getDescription(),
                getComputedThumbnail(playlist),
                playlist.isHidden(),
                videos,
                playlist.getCreatedAt()
        );
    }

    private String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Playlist cover file is empty.");
        }

        if (file.getSize() > 2 * 1024 * 1024) { //2mb max file size
            throw new IllegalArgumentException("Playlist cover size exceeds 2MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Invalid playlist cover type. Only jpg, png, and webp are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        try {
            Path targetPath = this.coverStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath);
            return "/uploads/playlist-covers/" + newFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store playlist cover. Please try again!", ex);
        }
    }
}
