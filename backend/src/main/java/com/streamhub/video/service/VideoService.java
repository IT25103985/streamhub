package com.streamhub.video.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final Path fileStorageLocation;

    public VideoService(VideoRepository videoRepository, @Value("${file.upload-dir}") String uploadDir) {
        this.videoRepository = videoRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public VideoResponseDTO uploadVideo(String title, String description, MultipartFile file, User owner) {
        // TODO(Chandrasena): validate file type/size before storing
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        try {
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation);
            
            Video video = new Video(title, description, targetLocation.toString(), owner);
            Video savedVideo = videoRepository.save(video);
            return mapToDTO(savedVideo);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + newFilename + ". Please try again!", ex);
        }
    }

    public Resource loadVideoAsResource(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
                
        Path filePath = Paths.get(video.getFilePath()).normalize();
        Resource resource = new FileSystemResource(filePath);
        
        if (resource.exists()) {
            return resource;
        } else {
            throw new RuntimeException("File not found " + video.getFilePath());
        }
    }

    public List<VideoResponseDTO> getAllVideos() {
        return videoRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public VideoResponseDTO getVideoDetails(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
        return mapToDTO(video);
    }

    public void deleteVideo(UUID videoId, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
                
        if (!video.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only delete your own videos");
        }
        
        // TODO(Chandrasena): cascade-safety check before delete
        
        try {
            Files.deleteIfExists(Paths.get(video.getFilePath()));
        } catch (IOException e) {
            System.err.println("Could not delete physical file: " + e.getMessage());
        }
        
        videoRepository.delete(video);
    }

    public void moderateDeleteVideo(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
                
        try {
            Files.deleteIfExists(Paths.get(video.getFilePath()));
        } catch (IOException e) {
            System.err.println("Could not delete physical file: " + e.getMessage());
        }
        
        videoRepository.delete(video);
    }
    
    private VideoResponseDTO mapToDTO(Video video) {
        return new VideoResponseDTO(
                video.getVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getOwner().getUserId(),
                video.getOwner().getUsername(),
                video.getCreatedAt()
        );
    }
}
