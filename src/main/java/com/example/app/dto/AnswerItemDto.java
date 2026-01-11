package com.example.app.dto;

import java.util.List;

public class AnswerItemDto {
	private Long questionId;
	private String questionType;

	private Long optionId;           // SINGLE
	private List<Long> optionIds;    // MULTI
	private String answerText;       // TEXT

	public Long getQuestionId() { return questionId; }
	public void setQuestionId(Long questionId) { this.questionId = questionId; }

	public String getQuestionType() { return questionType; }
	public void setQuestionType(String questionType) { this.questionType = questionType; }

	public Long getOptionId() { return optionId; }
	public void setOptionId(Long optionId) { this.optionId = optionId; }

	public List<Long> getOptionIds() { return optionIds; }
	public void setOptionIds(List<Long> optionIds) { this.optionIds = optionIds; }

	public String getAnswerText() { return answerText; }
	public void setAnswerText(String answerText) { this.answerText = answerText; }
}
