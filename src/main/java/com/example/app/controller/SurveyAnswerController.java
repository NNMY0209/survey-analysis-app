package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.dto.AnswerSubmitRequestDto;
import com.example.app.service.SurveyAnswerService;

@Controller
@RequestMapping("/survey")
public class SurveyAnswerController {

	private final SurveyAnswerService surveyAnswerService;

	public SurveyAnswerController(SurveyAnswerService surveyAnswerService) {
		this.surveyAnswerService = surveyAnswerService;
	}

	/** 回答画面表示（response_id を発行して、設問＋選択肢を表示） */
	@GetMapping("/{surveyId}/answer")
	public String showAnswer(@PathVariable long surveyId, Model model) {
		var vm = surveyAnswerService.buildAnswerView(surveyId);
		model.addAttribute("vm", vm);
		return "survey/answer";
	}

	/** 回答送信（SINGLE/MULTI/TEXT を question_answers に保存） */
	@PostMapping("/{surveyId}/submit")
	public String submit(@PathVariable long surveyId, @ModelAttribute AnswerSubmitRequestDto req) {
		// PathのsurveyIdを正として上書き（改ざん対策の最低限）
		req.setSurveyId(surveyId);

		surveyAnswerService.submit(req);

		return "redirect:/survey/" + surveyId + "/thanks";
	}

	/** 完了画面 */
	@GetMapping("/{surveyId}/thanks")
	public String thanks(@PathVariable long surveyId) {
		return "survey/thanks";
	}
}
