package com.streamhub.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//payload for updating playlist metadata privacy and thumbnail
public class UpdatePlaylistDTO {

    @NotBlank(message = "Playlist name is required")
    @Size(max = 255, message = "Playlist name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Privacy is required")
    private String privacy;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    private boolean removeThumbnail;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isRemoveThumbnail() {
        return removeThumbnail;
    }
    
    public void setRemoveThumbnail(boolean removeThumbnail) {
        this.removeThumbnail = removeThumbnail;
    }
}
