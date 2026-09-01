package com.streamhub.video.dto;

import java.util.UUID;

//payload for editing video title description category and privacy
public class VideoUpdateDTO {
    private String title;
    private String description;
    private UUID categoryId;
    private String privacy;

    public VideoUpdateDTO() {}

    public VideoUpdateDTO(String title, String description, UUID categoryId) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
    }

    public VideoUpdateDTO(String title, String description, UUID categoryId, String privacy) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.privacy = privacy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }
}
