package com.example.app.dto;

public class QuestionCountDto {
	private long questionId;
	private int displayOrder;
	private String questionText;
	private long answerCount;

	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}

	public int getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(int displayOrder) {
		this.displayOrder = displayOrder;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public long getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(long answerCount) {
		this.answerCount = answerCount;
	}
}
