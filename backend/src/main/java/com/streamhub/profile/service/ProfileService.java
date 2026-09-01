package com.streamhub.profile.service;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.profile.dto.ProfileResponseDTO;
import com.streamhub.profile.dto.ProfileUpdateDTO;
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
import java.util.UUID;

@Service
//handles profile data updates and saving avatar cover images
public class ProfileService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final Path avatarStorageLocation;
    private final Path coverStorageLocation;

    public ProfileService(UserRepository userRepository, 
                          VideoRepository videoRepository,
                          @Value("${file.avatar-upload-dir:uploads/avatars}") String avatarUploadDir,
                          @Value("${file.cover-upload-dir:uploads/covers}") String coverUploadDir) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.avatarStorageLocation = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        this.coverStorageLocation = Paths.get(coverUploadDir).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.avatarStorageLocation);
            Files.createDirectories(this.coverStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create directories for profile uploads.", ex);
        }
    }

    public ProfileResponseDTO getMyProfile(User currentUser) {
        long videoCount = videoRepository.countByOwner_UserId(currentUser.getUserId());
        return new ProfileResponseDTO(
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getEmail(),
                currentUser.getBio(),
                currentUser.getAvatarUrl(),
                currentUser.getCoverImageUrl(),
                videoCount
        );
    }

    public ProfileResponseDTO getPublicProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));
        long videoCount = videoRepository.countByOwner_UserIdAndIsHiddenFalse(user.getUserId());
        return new ProfileResponseDTO(
                user.getFirstName(),
                user.getLastName(),
                null, //hide email on public profiles
                user.getBio(),
                user.getAvatarUrl(),
                user.getCoverImageUrl(),
                videoCount
        );
    }

    //profile metadata update logic
    public ProfileResponseDTO updateProfile(User currentUser, ProfileUpdateDTO dto) {
        currentUser.setFirstName(dto.getFirstName());
        currentUser.setLastName(dto.getLastName());
        currentUser.setBio(dto.getBio());
        
        userRepository.save(currentUser);
        return getMyProfile(currentUser);
    }

    //avatar image validation and storage logic
    public ProfileResponseDTO uploadAvatar(User currentUser, MultipartFile file) {
        validateImageFile(file, 2 * 1024 * 1024, "Avatar"); //2mb max limit
        
        String filename = saveFile(file, avatarStorageLocation);
        currentUser.setAvatarUrl("/api/v1/users/avatar/" + filename);
        userRepository.save(currentUser);
        
        return getMyProfile(currentUser);
    }

    //cover image validation and storage logic
    public ProfileResponseDTO uploadCover(User currentUser, MultipartFile file) {
        validateImageFile(file, 5 * 1024 * 1024, "Cover image"); //5mb max limit
        
        String filename = saveFile(file, coverStorageLocation);
        currentUser.setCoverImageUrl("/api/v1/users/cover/" + filename);
        userRepository.save(currentUser);
        
        return getMyProfile(currentUser);
    }

    public Resource loadAvatarAsResource(String filename) {
        return loadFileAsResource(filename, avatarStorageLocation);
    }

    public Resource loadCoverAsResource(String filename) {
        return loadFileAsResource(filename, coverStorageLocation);
    }

    private void validateImageFile(MultipartFile file, long maxSize, String fieldName) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " file is empty.");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(fieldName + " size exceeds the allowed limit.");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) {
            throw new IllegalArgumentException(fieldName + " must be a JPG, PNG, or WEBP image.");
        }
    }

    private String saveFile(MultipartFile file, Path targetLocation) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        try {
            Path targetPath = targetLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath);
            return newFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + newFilename + ". Please try again!", ex);
        }
    }

    private Resource loadFileAsResource(String filename, Path storageLocation) {
        try {
            Path filePath = storageLocation.resolve(filename).normalize();
            if (!filePath.startsWith(storageLocation)) {
                throw new SecurityException("Access denied: Invalid file path");
            }
            Resource resource = new FileSystemResource(filePath);
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + filename);
            }
        } catch (SecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("File not found " + filename, ex);
        }
    }
}
