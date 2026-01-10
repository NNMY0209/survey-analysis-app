package com.example.app.dto;

public class QuestionDto {
	private long questionId;
	private long surveyId;
	private String questionText;
	private String questionType;
	private String questionRole;
	private int isReverse;
	private int isRequired;
	private int displayOrder;

	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}

	public long getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(long surveyId) {
		this.surveyId = surveyId;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public String getQuestionType() {
		return questionType;
	}

	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}

	public String getQuestionRole() {
		return questionRole;
	}

	public void setQuestionRole(String questionRole) {
		this.questionRole = questionRole;
	}

	public int getIsReverse() {
		return isReverse;
	}

	public void setIsReverse(int isReverse) {
		this.isReverse = isReverse;
	}

	public int getIsRequired() {
		return isRequired;
	}

	public void setIsRequired(int isRequired) {
		this.isRequired = isRequired;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}
}
