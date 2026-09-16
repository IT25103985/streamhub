package com.streamhub.help.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

//dto for creating or updating a help topic entry
public class HelpTopicRequest {

    @NotBlank(message = "Section is required")
    @Size(max = 150, message = "Section must not exceed 150 characters")
    private String section;

    @NotBlank(message = "Question is required")
    @Size(max = 255, message = "Question must not exceed 255 characters")
    private String question;

    @NotBlank(message = "Answer is required")
    @Size(max = 2000, message = "Answer must not exceed 2000 characters")
    private String answer;

    @NotNull(message = "Display order is required")
    @Min(value = 0, message = "Display order must be 0 or greater")
    private Integer displayOrder = 0;

    public HelpTopicRequest() {
    }

    public HelpTopicRequest(String section, String question, String answer, Integer displayOrder) {
        this.section = section;
        this.question = question;
        this.answer = answer;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
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
}
