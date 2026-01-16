package com.example.app.dto;

public class ScaleAvgDto {

	private Long scaleId;
	private String scaleCode;
	private String scaleName;

	/** 回答者数（COMPLETEDのみ） */
	private Long respondentCount; // N

	/** 平均値（重み・逆転込み） */
	private Double avgScore;

	/** 標準偏差 */
	private Double sdScore;

	/** 中央値 */
	private Double medianScore;

	public Long getScaleId() {
		return scaleId;
	}

	public void setScaleId(Long scaleId) {
		this.scaleId = scaleId;
	}

	public String getScaleCode() {
		return scaleCode;
	}

	public void setScaleCode(String scaleCode) {
		this.scaleCode = scaleCode;
	}

	public String getScaleName() {
		return scaleName;
	}

	public void setScaleName(String scaleName) {
		this.scaleName = scaleName;
	}

	public Long getRespondentCount() {
		return respondentCount;
	}

	public void setRespondentCount(Long respondentCount) {
		this.respondentCount = respondentCount;
	}

	public Double getAvgScore() {
		return avgScore;
	}

	public void setAvgScore(Double avgScore) {
		this.avgScore = avgScore;
	}

	public Double getSdScore() {
		return sdScore;
	}

	public void setSdScore(Double sdScore) {
		this.sdScore = sdScore;
	}

	public Double getMedianScore() {
		return medianScore;
	}

	public void setMedianScore(Double medianScore) {
		this.medianScore = medianScore;
	}
}
