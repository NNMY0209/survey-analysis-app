package com.example.app.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dto.AdminResponseRowDto;

@Controller
@RequestMapping("/admin")
public class AdminResponseController {

	private final AdminResponseDao adminResponseDao;

	public AdminResponseController(AdminResponseDao adminResponseDao) {
		this.adminResponseDao = adminResponseDao;
	}

	/**
	 * 回答一覧（デフォルトはCOMPLETEDのみ）
	 * 例:
	 *  /admin/surveys/1/responses            -> COMPLETEDのみ
	 *  /admin/surveys/1/responses?status=all -> 全件（IN_PROGRESS含む）
	 */
	@GetMapping("/surveys/{surveyId}/responses")
	public String listResponses(
			@PathVariable long surveyId,
			@RequestParam(name = "status", required = false, defaultValue = "completed") String status,
			Model model) {
		boolean completedOnly = !"all".equalsIgnoreCase(status);

		List<AdminResponseRowDto> responses = adminResponseDao.findResponseList(surveyId, completedOnly);

		model.addAttribute("surveyId", surveyId);
		model.addAttribute("status", status);
		model.addAttribute("responses", responses);

		return "admin/response-list";
	}
}
