package com.example.app.dto;

import java.sql.Timestamp;

public class SurveyDetailDto {
	private long surveyId;
	private String title;
	private String status;
	private Timestamp openAt;
	private Timestamp closeAt;
	private String description;
	private String consentText;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getConsentText() {
		return consentText;
	}

	public void setConsentText(String consentText) {
		this.consentText = consentText;
	}
}
