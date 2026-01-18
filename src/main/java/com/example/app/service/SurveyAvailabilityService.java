package com.example.app.service;

import java.sql.Timestamp;

import org.springframework.stereotype.Service;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.SurveyDetailDto;

@Service
public class SurveyAvailabilityService {

	private final SurveyDao surveyDao;

	public SurveyAvailabilityService(SurveyDao surveyDao) {
		this.surveyDao = surveyDao;
	}

	public SurveyAvailability check(long surveyId) {
	    SurveyDetailDto s = surveyDao.findPublishSettingsById(surveyId);

	    // 1) 手動停止が最優先
	    if ("CLOSED".equalsIgnoreCase(s.getStatus())) {
	        return SurveyAvailability.CLOSED; // 手動停止もCLOSED扱いでOK
	    }

	    Timestamp now = new Timestamp(System.currentTimeMillis());

	    // 2) 開始前
	    if (s.getOpenAt() != null && now.before(s.getOpenAt())) {
	        return SurveyAvailability.NOT_OPEN_YET;
	    }

	    // 3) 終了
	    if (s.getCloseAt() != null && !now.before(s.getCloseAt())) {
	        return SurveyAvailability.CLOSED;
	    }

	    // 4) 期間内 → OK（statusがDRAFTでもOKになる＝自動公開）
	    return SurveyAvailability.OK;
	}


	public SurveyDetailDto getInfo(long surveyId) {
		return surveyDao.findPublishSettingsById(surveyId);
	}
}
