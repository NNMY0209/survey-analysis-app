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
import com.example.app.service.AnswerImportService;

@Controller
@RequestMapping("/admin/surveys")
public class AdminAnswerImportController {

	private final AnswerImportService importService;
	private final SurveyDao surveyDao;

	public AdminAnswerImportController(
			AnswerImportService importService,
			SurveyDao surveyDao) {
		this.importService = importService;
		this.surveyDao = surveyDao;
	}

	/** CSVアップロード画面 */
	@GetMapping("/{surveyId}/responses/import")
	public String showImportPage(@PathVariable long surveyId, Model model) {

		var survey = surveyDao.findDetailById(surveyId);
		model.addAttribute("survey", survey);

		return "admin/answer-import";
	}

	/** CSV取り込み実行 */
	@PostMapping("/{surveyId}/responses/import")
	public String importCsv(
			@PathVariable long surveyId,
			@RequestParam("file") MultipartFile file,
			Model model) {

		var survey = surveyDao.findDetailById(surveyId);
		model.addAttribute("survey", survey);

		if (file == null || file.isEmpty()) {
			model.addAttribute("error", "CSVファイルを選択してください");
			return "admin/answer-import";
		}

		ImportResultDto result = importService.importCsv(surveyId, file);
		model.addAttribute("result", result);

		return "admin/answer-import-result";
	}
}
