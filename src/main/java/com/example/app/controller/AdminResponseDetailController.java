package com.example.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.app.dao.AdminResponseDetailDao;

@Controller
@RequestMapping("/admin")
public class AdminResponseDetailController {

    private final AdminResponseDetailDao detailDao;

    public AdminResponseDetailController(AdminResponseDetailDao detailDao) {
        this.detailDao = detailDao;
    }

    @GetMapping("/surveys/{surveyId}/responses/{responseId}")
    public String showDetail(@PathVariable long surveyId,@PathVariable long responseId, Model model) {

        var details = detailDao.findDetailsByResponseId(responseId);

        model.addAttribute("surveyId", surveyId);
        model.addAttribute("responseId", responseId);
        model.addAttribute("details", details);

        return "admin/response-detail";
    }
}
