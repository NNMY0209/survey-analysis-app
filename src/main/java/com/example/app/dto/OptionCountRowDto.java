package com.example.app.dto;

public class OptionCountRowDto {
	private long questionId;
	private int questionOrder;
	private String questionText;

	private long optionId;
	private int optionOrder;
	private String optionText;

	private long selectedCount;

	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}

	public int getQuestionOrder() {
		return questionOrder;
	}

	public void setQuestionOrder(int questionOrder) {
		this.questionOrder = questionOrder;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public long getOptionId() {
		return optionId;
	}

	public void setOptionId(long optionId) {
		this.optionId = optionId;
	}

	public int getOptionOrder() {
		return optionOrder;
	}

	public void setOptionOrder(int optionOrder) {
		this.optionOrder = optionOrder;
	}

	public String getOptionText() {
		return optionText;
	}

	public void setOptionText(String optionText) {
		this.optionText = optionText;
	}

	public long getSelectedCount() {
		return selectedCount;
	}

	public void setSelectedCount(long selectedCount) {
		this.selectedCount = selectedCount;
	}
}
