package com.streamhub.preferences.dto;

import java.util.List;
import java.util.UUID;

//request payload for onboarding category preferences
public class CategoryPreferencesRequestDTO {

    private List<UUID> categoryIds;

    public CategoryPreferencesRequestDTO() {
    }

    public CategoryPreferencesRequestDTO(List<UUID> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public List<UUID> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<UUID> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
