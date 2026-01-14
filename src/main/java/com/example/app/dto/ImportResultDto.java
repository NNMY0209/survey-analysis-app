package com.example.app.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResultDto {
	private int totalRows;
	private int successRows;
	private int errorRows;
	private List<String> errors = new ArrayList<>();

	public int getTotalRows() {
		return totalRows;
	}

	public void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}

	public int getSuccessRows() {
		return successRows;
	}

	public void setSuccessRows(int successRows) {
		this.successRows = successRows;
	}

	public int getErrorRows() {
		return errorRows;
	}

	public void setErrorRows(int errorRows) {
		this.errorRows = errorRows;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void addError(String msg) {
		this.errors.add(msg);
		this.errorRows++;
	}
}
