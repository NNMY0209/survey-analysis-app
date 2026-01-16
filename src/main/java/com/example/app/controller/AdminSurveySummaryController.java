package com.example.app.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.OptionCountRowDto;

@Controller
public class AdminSurveySummaryController {

	private final SurveyDao surveyDao;

	public AdminSurveySummaryController(SurveyDao surveyDao) {
		this.surveyDao = surveyDao;
	}

	@GetMapping("/admin/surveys/{surveyId}/summary")
	public String summary(@PathVariable long surveyId, Model model) {

		model.addAttribute("survey", surveyDao.findDetailById(surveyId));

		// A: 設問ごとの回答数
		model.addAttribute("questionCounts", surveyDao.findQuestionCountsBySurveyId(surveyId));

		// B: 選択肢ごとの人数（設問ごとにまとめる）
		List<OptionCountRowDto> rows = surveyDao.findOptionCountsBySurveyId(surveyId);
		Map<Long, List<OptionCountRowDto>> optionCountMap = new LinkedHashMap<>();
		for (OptionCountRowDto r : rows) {
			optionCountMap.computeIfAbsent(r.getQuestionId(), k -> new ArrayList<>()).add(r);
		}
		model.addAttribute("optionCountMap", optionCountMap);

		// C: 平均スコア（SINGLEのみ）
		model.addAttribute("avgScores", surveyDao.findAvgScoresBySurveyId(surveyId));

		// D: 下位尺度平均
		model.addAttribute("scaleAvgs", surveyDao.findScaleAveragesBySurveyId(surveyId));

		return "admin/survey-summary";
	}
}
