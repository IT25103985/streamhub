package com.streamhub.profile.dto;

//response object containing public or private profile details
public class ProfileResponseDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String avatarUrl;
    private String coverImageUrl;
    private long uploadedVideoCount;

    public ProfileResponseDTO() {
    }

    public ProfileResponseDTO(String firstName, String lastName, String email, String bio, String avatarUrl, String coverImageUrl, long uploadedVideoCount) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.coverImageUrl = coverImageUrl;
        this.uploadedVideoCount = uploadedVideoCount;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public long getUploadedVideoCount() { return uploadedVideoCount; }
    public void setUploadedVideoCount(long uploadedVideoCount) { this.uploadedVideoCount = uploadedVideoCount; }
}
