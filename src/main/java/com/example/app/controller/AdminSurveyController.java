package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.app.dao.SurveyDao;

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
		model.addAttribute("survey", surveyDao.findDetailById(surveyId));
		model.addAttribute("questions", surveyDao.findQuestionsBySurveyId(surveyId));
		return "admin/survey-detail";
	}
}
