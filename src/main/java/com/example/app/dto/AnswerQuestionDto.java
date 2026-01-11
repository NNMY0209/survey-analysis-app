package com.example.app.dto;

import java.util.List;

public class AnswerQuestionDto {
	private Long questionId;
	private String questionText;
	private String questionType;
	private List<OptionDto> options;

	public Long getQuestionId() { return questionId; }
	public void setQuestionId(Long questionId) { this.questionId = questionId; }

	public String getQuestionText() { return questionText; }
	public void setQuestionText(String questionText) { this.questionText = questionText; }

	public String getQuestionType() { return questionType; }
	public void setQuestionType(String questionType) { this.questionType = questionType; }

	public List<OptionDto> getOptions() { return options; }
	public void setOptions(List<OptionDto> options) { this.options = options; }
}
