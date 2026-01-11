package com.example.app.dto;

public class OptionDto {
    private long optionId;
    private long questionId;
    private String optionText;
    private Integer score;
    private int displayOrder;

    public long getOptionId() { return optionId; }
    public void setOptionId(long optionId) { this.optionId = optionId; }

    public long getQuestionId() { return questionId; }
    public void setQuestionId(long questionId) { this.questionId = questionId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
