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
import com.example.app.service.SurveyAvailability;
import com.example.app.service.SurveyAvailabilityService;

@Controller
@RequestMapping("/survey")
public class SurveyAnswerController {

	private final SurveyAnswerService surveyAnswerService;
	private final SurveyAvailabilityService availabilityService;

	public SurveyAnswerController(SurveyAnswerService surveyAnswerService,
			SurveyAvailabilityService availabilityService) {
		this.surveyAnswerService = surveyAnswerService;
		this.availabilityService = availabilityService;
	}

	@GetMapping("/{surveyId}/answer")
	public String showAnswer(@PathVariable long surveyId, Model model) {

		var av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		var vm = surveyAnswerService.buildAnswerView(surveyId);
		model.addAttribute("vm", vm);
		return "survey/answer";
	}

	@PostMapping("/{surveyId}/submit")
	public String submit(@PathVariable long surveyId, @ModelAttribute AnswerSubmitRequestDto req, Model model) {
		req.setSurveyId(surveyId);

		var av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		surveyAnswerService.submit(req);
		return "redirect:/survey/" + surveyId + "/thanks";
	}

	@GetMapping("/{surveyId}/thanks")
	public String thanks(@PathVariable long surveyId) {
		return "survey/thanks";
	}
}
