package com.example.app.controller;

import java.util.ArrayList;

//AdminSurveyQuestionController.java
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.app.dto.AdminQuestionCreateFormDto;
import com.example.app.service.QuestionAdminService;

@Controller
@RequestMapping("/admin/surveys/{surveyId}/questions")
public class AdminSurveyQuestionController {

	private final QuestionAdminService service;

	public AdminSurveyQuestionController(QuestionAdminService service) {
		this.service = service;
	}

	@GetMapping("/new")
	public String newForm(@PathVariable long surveyId, Model model) {
		AdminQuestionCreateFormDto form = new AdminQuestionCreateFormDto();

		// 初期表示用に選択肢行を5つ出す（好みで可変でもOK）
		for (int i = 0; i < 5; i++) {
			form.getOptions().add(new AdminQuestionCreateFormDto.OptionForm());
		}

		model.addAttribute("surveyId", surveyId);
		model.addAttribute("form", form);
		return "admin/question_new";
	}

	@PostMapping
	public String create(
			@PathVariable long surveyId,
			@ModelAttribute("form") AdminQuestionCreateFormDto form,
			Model model,
			RedirectAttributes ra) {
		try {
			service.createQuestion(surveyId, form);
			ra.addFlashAttribute("message", "設問を登録しました。");
			return "redirect:/admin/surveys/" + surveyId;
		} catch (IllegalArgumentException e) {
			model.addAttribute("surveyId", surveyId);
			model.addAttribute("error", e.getMessage());
			// options行が0だと再描画が辛いので保険
			if (form.getOptions() == null)
				form.setOptions(new ArrayList<>());
			if (form.getOptions().size() < 2) {
				while (form.getOptions().size() < 5)
					form.getOptions().add(new AdminQuestionCreateFormDto.OptionForm());
			}
			return "admin/question_new";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/admin/surveys/" + surveyId;
		}
	}
}
