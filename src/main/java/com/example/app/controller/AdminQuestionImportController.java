package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.ImportResultDto;
import com.example.app.service.QuestionImportService;

@Controller
@RequestMapping("/admin/surveys")
public class AdminQuestionImportController {

	private final QuestionImportService importService;
	private final SurveyDao surveyDao;

	public AdminQuestionImportController(QuestionImportService importService, SurveyDao surveyDao) {
		this.importService = importService;
		this.surveyDao = surveyDao;
	}
	
	@GetMapping("/{surveyId}/import")
	public String showImportPage(@PathVariable long surveyId, Model model) {
	    model.addAttribute("survey", surveyDao.findDetailById(surveyId));
	    model.addAttribute("surveyId", surveyId);
	    return "admin/import";
	}


	// ==========================
	// 新：統合ページ（1ページ）向け
	// ==========================

	/** 設問のみインポート（questions.csv） */
	@PostMapping("/{surveyId}/questions/import/questions")
	public String importQuestions(
			@PathVariable long surveyId,
			@RequestParam("questionsFile") MultipartFile questionsFile,
			Model model) {
		model.addAttribute("survey", surveyDao.findDetailById(surveyId));
		model.addAttribute("surveyId", surveyId);

		if (questionsFile == null || questionsFile.isEmpty()) {
			ImportResultDto r = new ImportResultDto();
			r.addError("questions.csv を選択してください");
			model.addAttribute("questionsResult", r);
			return "admin/import"; // 統合ページへ戻す
		}

		// ★ Serviceに「設問だけ」用メソッドが必要
		ImportResultDto result = importService.importQuestionsOnly(surveyId, questionsFile);
		model.addAttribute("questionsResult", result);

		return "admin/import";
	}

	/** 選択肢のみインポート（question_options.csv） */
	@PostMapping("/{surveyId}/questions/import/options")
	public String importOptions(
			@PathVariable long surveyId,
			@RequestParam("optionsFile") MultipartFile optionsFile,
			Model model) {
		model.addAttribute("survey", surveyDao.findDetailById(surveyId));
		model.addAttribute("surveyId", surveyId);

		if (optionsFile == null || optionsFile.isEmpty()) {
			ImportResultDto r = new ImportResultDto();
			r.addError("question_options.csv を選択してください");
			model.addAttribute("optionsResult", r);
			return "admin/import";
		}

		// ★ Serviceに「選択肢だけ」用メソッドが必要
		ImportResultDto result = importService.importOptionsOnly(surveyId, optionsFile);
		model.addAttribute("optionsResult", result);

		return "admin/import";
	}

}
