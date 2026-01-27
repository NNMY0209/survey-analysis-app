package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.ImportResultDto;
import com.example.app.service.ScaleImportService;

@Controller
@RequestMapping("/admin/surveys")
public class AdminScaleImportController {

	private final ScaleImportService scaleImportService;
	private final SurveyDao surveyDao;

	public AdminScaleImportController(ScaleImportService scaleImportService, SurveyDao surveyDao) {
		this.scaleImportService = scaleImportService;
		this.surveyDao = surveyDao;
	}

	@PostMapping("/{surveyId}/scales/import/scales")
	public String importScales(@PathVariable long surveyId,
			@RequestParam("scalesFile") MultipartFile scalesFile,
			Model model) {

		model.addAttribute("survey", surveyDao.findDetailById(surveyId));
		model.addAttribute("surveyId", surveyId);

		ImportResultDto result = scaleImportService.importScalesOnly(surveyId, scalesFile);
		model.addAttribute("scalesResult", result);

		return "admin/import"; // 統合ページへ戻す
	}

	@PostMapping("/{surveyId}/scales/import/weights")
	public String importWeights(@PathVariable long surveyId,
			@RequestParam("scaleQuestionsFile") MultipartFile scaleQuestionsFile,
			Model model) {

		model.addAttribute("survey", surveyDao.findDetailById(surveyId));
		model.addAttribute("surveyId", surveyId);

		ImportResultDto result = scaleImportService.importScaleQuestionsOnly(surveyId, scaleQuestionsFile);
		model.addAttribute("weightsResult", result);

		return "admin/import"; // 統合ページへ戻す
	}

}
