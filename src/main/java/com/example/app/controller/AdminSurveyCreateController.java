package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.AdminSurveyCreateFormDto;

@Controller
@RequestMapping("/admin/surveys")
public class AdminSurveyCreateController {

	private final SurveyDao surveyDao;

	public AdminSurveyCreateController(SurveyDao surveyDao) {
		this.surveyDao = surveyDao;
	}

	@GetMapping("/new")
	public String show(Model model) {
		model.addAttribute("form", new AdminSurveyCreateFormDto());
		return "admin/survey-new";
	}

	@PostMapping
	public String create(@ModelAttribute("form") AdminSurveyCreateFormDto form, Model model) {

		String title = (form.getTitle() == null) ? "" : form.getTitle().trim();
		if (title.isBlank()) {
			model.addAttribute("error", "アンケート名を入力してください");
			return "admin/survey-new";
		}

		long adminUserId = 1; // TODO: ログイン実装後にセッションから取得
		long surveyId = surveyDao.insertDraft(title, adminUserId);

		return "redirect:/admin/surveys/" + surveyId;
	}
}
