package com.streamhub.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

//payload for adding a new top level comment or reply
public class CreateCommentDTO {

    @NotBlank(message = "Comment text is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String text;

    private java.util.UUID parentCommentId;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public java.util.UUID getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(java.util.UUID parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}
