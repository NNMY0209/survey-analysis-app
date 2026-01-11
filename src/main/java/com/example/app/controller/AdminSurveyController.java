package com.example.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionDto;
import com.example.app.dto.SurveyDetailDto;

@Controller
public class AdminSurveyController {

	private final SurveyDao surveyDao;

	public AdminSurveyController(SurveyDao surveyDao) {
		this.surveyDao = surveyDao;
	}

	@GetMapping("/admin/surveys")
	public String surveys(Model model) {
		model.addAttribute("surveys", surveyDao.findAll());
		return "admin/surveys";
	}

	@GetMapping("/admin/surveys/{surveyId}")
	public String surveyDetail(@PathVariable long surveyId, Model model) {
		SurveyDetailDto survey = surveyDao.findDetailById(surveyId);
		List<QuestionDto> questions = surveyDao.findQuestionsBySurveyId(surveyId);
		Map<Long, List<OptionDto>> optionMap = surveyDao.findOptionsBySurveyId(surveyId);
		model.addAttribute("survey", survey);
		model.addAttribute("questions", questions);
		model.addAttribute("optionMap", optionMap);
		return "admin/survey-detail";
	}
}
