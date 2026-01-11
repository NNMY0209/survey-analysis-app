package com.example.app.dto;

import java.util.List;

public class AnswerViewModel {
	private Long surveyId;
	private Long responseId;
	private List<AnswerQuestionDto> questions;

	public Long getSurveyId() { return surveyId; }
	public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

	public Long getResponseId() { return responseId; }
	public void setResponseId(Long responseId) { this.responseId = responseId; }

	public List<AnswerQuestionDto> getQuestions() { return questions; }
	public void setQuestions(List<AnswerQuestionDto> questions) { this.questions = questions; }
}

