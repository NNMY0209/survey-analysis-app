package com.example.app.dto;

public class ScaleDto {
  private long scaleId;
  private String scaleCode;
  private String scaleName;
  private String description;

  public long getScaleId() { return scaleId; }
  public void setScaleId(long scaleId) { this.scaleId = scaleId; }

  public String getScaleCode() { return scaleCode; }
  public void setScaleCode(String scaleCode) { this.scaleCode = scaleCode; }

  public String getScaleName() { return scaleName; }
  public void setScaleName(String scaleName) { this.scaleName = scaleName; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
}

