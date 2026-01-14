package com.example.app.dto;

import java.util.Map;

public class ImportCsvRowDto {
	private String respondentKey;
	// "Q1","Q2","Q3" -> cell value
	private Map<String, String> answersByColumn;

	public ImportCsvRowDto(String respondentKey, Map<String, String> answersByColumn) {
		this.respondentKey = respondentKey;
		this.answersByColumn = answersByColumn;
	}

	public String getRespondentKey() {
		return respondentKey;
	}

	public Map<String, String> getAnswersByColumn() {
		return answersByColumn;
	}
}
