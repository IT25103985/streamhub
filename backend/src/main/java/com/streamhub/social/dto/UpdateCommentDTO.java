package com.streamhub.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//payload for editing existing comment text
public class UpdateCommentDTO {

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
