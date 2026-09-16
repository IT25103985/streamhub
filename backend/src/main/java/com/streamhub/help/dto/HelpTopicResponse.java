package com.streamhub.help.dto;

import com.streamhub.help.entity.HelpTopic;

import java.time.LocalDateTime;
import java.util.UUID;

//dto for returning a help topic response to the client
public class HelpTopicResponse {

    private UUID id;
    private String section;
    private String question;
    private String answer;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HelpTopicResponse() {
    }

    public HelpTopicResponse(UUID id, String section, String question, String answer, Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.section = section;
        this.question = question;
        this.answer = answer;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static HelpTopicResponse fromEntity(HelpTopic entity) {
        if (entity == null) return null;
        return new HelpTopicResponse(
                entity.getId(),
                entity.getSection(),
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getDisplayOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
