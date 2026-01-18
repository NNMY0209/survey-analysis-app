package com.example.app.dto;

import java.sql.Timestamp;

public class AdminSurveyRowDto {
	private long surveyId;
	private String title;
	private String status;
	private Timestamp openAt;
	private Timestamp closeAt;
	private int answerCount;

	public long getSurveyId() {
		return surveyId;
	}

	public void setSurveyId(long surveyId) {
		this.surveyId = surveyId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Timestamp getOpenAt() {
		return openAt;
	}

	public void setOpenAt(Timestamp openAt) {
		this.openAt = openAt;
	}

	public Timestamp getCloseAt() {
		return closeAt;
	}

	public void setCloseAt(Timestamp closeAt) {
		this.closeAt = closeAt;
	}

	public int getAnswerCount() {
		return answerCount;
	}

	public void setAnswerCount(int answerCount) {
		this.answerCount = answerCount;
	}

	private String displayStatus; // 一覧表示用

	public String getDisplayStatus() {
		return displayStatus;
	}

	public void setDisplayStatus(String displayStatus) {
		this.displayStatus = displayStatus;
	}

	private String displayStatusClass;

	public String getDisplayStatusClass() {
		return displayStatusClass;
	}

	public void setDisplayStatusClass(String displayStatusClass) {
		this.displayStatusClass = displayStatusClass;
	}

}
