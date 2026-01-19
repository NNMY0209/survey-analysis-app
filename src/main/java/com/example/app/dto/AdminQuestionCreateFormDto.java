package com.example.app.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminQuestionCreateFormDto {

	// ===== Question =====
	private String questionText;
	private String questionType = "SINGLE_CHOICE";
	private String questionRole = "NORMAL"; // NORMAL / ATTENTION_CHECK / VALIDITY_CHECK
	private boolean reverse;
	private boolean required = true;

	// 表示順はUIからは使わない（Serviceで自動採番）
	// private Integer displayOrder; ← 削除 or 無視

	// ===== Option =====
	private String optionTemplate = "LIKERT5";
	// LIKERT5 / LIKERT7 / CUSTOM

	private List<OptionForm> options = new ArrayList<>();

	public static class OptionForm {
		private String optionText;
		private Integer score; // CUSTOM のときのみ使用
		private boolean correct; // ATTENTION_CHECK のときのみ意味を持つ

		public String getOptionText() {
			return optionText;
		}

		public void setOptionText(String optionText) {
			this.optionText = optionText;
		}

		public Integer getScore() {
			return score;
		}

		public void setScore(Integer score) {
			this.score = score;
		}

		public boolean isCorrect() {
			return correct;
		}

		public void setCorrect(boolean correct) {
			this.correct = correct;
		}
	}

	// getters / setters
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

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getOptionTemplate() {
		return optionTemplate;
	}

	public void setOptionTemplate(String optionTemplate) {
		this.optionTemplate = optionTemplate;
	}

	public List<OptionForm> getOptions() {
		return options;
	}

	public void setOptions(List<OptionForm> options) {
		this.options = options;
	}

}
