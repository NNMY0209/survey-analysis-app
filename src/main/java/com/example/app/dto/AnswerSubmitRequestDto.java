package com.example.app.dto;

import java.util.List;

public class AnswerSubmitRequestDto {
	private Long surveyId;
	private Long responseId;
	private List<AnswerItemDto> answers;

	public Long getSurveyId() { return surveyId; }
	public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

	public Long getResponseId() { return responseId; }
	public void setResponseId(Long responseId) { this.responseId = responseId; }

	public List<AnswerItemDto> getAnswers() { return answers; }
	public void setAnswers(List<AnswerItemDto> answers) { this.answers = answers; }
}
