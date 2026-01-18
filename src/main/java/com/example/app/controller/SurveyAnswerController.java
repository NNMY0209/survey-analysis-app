package com.example.app.controller;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.dao.SurveyDao;
import com.example.app.dto.AnswerSubmitRequestDto;
import com.example.app.service.SurveyAnswerService;
import com.example.app.service.SurveyAvailability;
import com.example.app.service.SurveyAvailabilityService;

@Controller
@RequestMapping("/survey")
public class SurveyAnswerController {

	private final SurveyAnswerService surveyAnswerService;
	private final SurveyAvailabilityService availabilityService;
	private final SurveyDao surveyDao;
	private final HttpSession session;
	private static final String DEFAULT_DESCRIPTION =
	        "本アンケートは学習目的で実施しています。所要時間はおよそ5〜10分です。途中で中断しても問題ありません。";

	private static final String DEFAULT_CONSENT =
	        "以下を確認のうえ、同意いただける場合のみ回答を開始してください。\n"
	      + "・回答は任意です。\n"
	      + "・個人が特定される情報の入力は避けてください。\n"
	      + "・集計結果は学習目的で利用します。";

	public SurveyAnswerController(SurveyAnswerService surveyAnswerService,
			SurveyAvailabilityService availabilityService,
			SurveyDao surveyDao,
			HttpSession session) {
		this.surveyAnswerService = surveyAnswerService;
		this.availabilityService = availabilityService;
		this.surveyDao = surveyDao;
		this.session = session;
	}

	// ===== 開始（同意文・説明文）=====
	@GetMapping("/{surveyId}")
	public String start(@PathVariable long surveyId, Model model) {

		SurveyAvailability av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		var dto = surveyDao.findConsentById(surveyId);

		String descriptionForDisplay = (dto.getDescription() == null || dto.getDescription().trim().isEmpty())
				? DEFAULT_DESCRIPTION
				: dto.getDescription();

		String consentForDisplay = (dto.getConsentText() == null || dto.getConsentText().trim().isEmpty())
				? DEFAULT_CONSENT
				: dto.getConsentText();

		model.addAttribute("survey", dto);
		model.addAttribute("descriptionForDisplay", descriptionForDisplay);
		model.addAttribute("consentForDisplay", consentForDisplay);

		return "survey/start";
	}

	// ===== 同意して開始 =====
	@PostMapping("/{surveyId}/consent")
	public String consent(@PathVariable long surveyId,
			@RequestParam(name = "agree", required = false) String agree,
			Model model) {

		SurveyAvailability av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		if (agree == null) {
			var dto = surveyDao.findConsentById(surveyId);
			model.addAttribute("survey", dto);
			model.addAttribute("error", "同意にチェックしてください。");
			return "survey/start";
		}

		session.setAttribute(consentKey(surveyId), Boolean.TRUE);
		return "redirect:/survey/" + surveyId + "/answer";
	}

	/** 回答画面表示 */
	@GetMapping("/{surveyId}/answer")
	public String showAnswer(@PathVariable long surveyId, Model model) {

		if (!isConsented(surveyId)) {
			return "redirect:/survey/" + surveyId;
		}

		SurveyAvailability av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		var vm = surveyAnswerService.buildAnswerView(surveyId);
		model.addAttribute("vm", vm);
		return "survey/answer";
	}

	/** 回答送信 */
	@PostMapping("/{surveyId}/submit")
	public String submit(@PathVariable long surveyId,
			@ModelAttribute AnswerSubmitRequestDto req,
			Model model) {

		if (!isConsented(surveyId)) {
			return "redirect:/survey/" + surveyId;
		}

		req.setSurveyId(surveyId);

		SurveyAvailability av = availabilityService.check(surveyId);
		if (av != SurveyAvailability.OK) {
			model.addAttribute("availability", av);
			model.addAttribute("info", availabilityService.getInfo(surveyId));
			return "survey/out_of_period";
		}

		surveyAnswerService.submit(req);

		// 1回きりにしたいなら同意フラグを消す
		session.removeAttribute(consentKey(surveyId));

		return "redirect:/survey/" + surveyId + "/thanks";
	}

	@GetMapping("/{surveyId}/thanks")
	public String thanks(@PathVariable long surveyId) {
		return "survey/thanks";
	}

	private String consentKey(long surveyId) {
		return "CONSENTED_SURVEY_" + surveyId;
	}

	private boolean isConsented(long surveyId) {
		Object v = session.getAttribute(consentKey(surveyId));
		return (v instanceof Boolean) && ((Boolean) v);
	}
}
