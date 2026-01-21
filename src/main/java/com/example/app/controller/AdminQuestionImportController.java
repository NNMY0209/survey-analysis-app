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

    /** 設問定義CSVアップロード画面（2ファイル） */
    @GetMapping("/{surveyId}/questions/import")
    public String show(@PathVariable long surveyId, Model model) {
        model.addAttribute("survey", surveyDao.findDetailById(surveyId));
        return "admin/question-import";
    }

    /** CSV取り込み実行（questions.csv + question_options.csv） */
    @PostMapping("/{surveyId}/questions/import")
    public String importCsv(
            @PathVariable long surveyId,
            @RequestParam("questionsFile") MultipartFile questionsFile,
            @RequestParam("optionsFile") MultipartFile optionsFile,
            Model model
    ) {
        model.addAttribute("survey", surveyDao.findDetailById(surveyId));

        if (questionsFile == null || questionsFile.isEmpty()) {
            model.addAttribute("error", "questions.csv を選択してください");
            return "admin/question-import";
        }
        if (optionsFile == null || optionsFile.isEmpty()) {
            model.addAttribute("error", "question_options.csv を選択してください");
            return "admin/question-import";
        }

        ImportResultDto result = importService.importCsv(surveyId, questionsFile, optionsFile);
        model.addAttribute("result", result);

        return "admin/question-import-result";
    }
}
