package com.example.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dao.SurveyDao;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionDto;
import com.example.app.dto.SurveyDetailDto;

@Controller
public class AdminSurveyController {

    private final SurveyDao surveyDao;
    private final AdminResponseDao adminResponseDao;

    public AdminSurveyController(SurveyDao surveyDao, AdminResponseDao adminResponseDao) {
        this.surveyDao = surveyDao;
        this.adminResponseDao = adminResponseDao;
    }

    @GetMapping("/admin/surveys")
    public String surveys(Model model) {
        model.addAttribute("surveys", surveyDao.findAll());
        return "admin/surveys";
    }

    @GetMapping("/admin/surveys/{surveyId}")
    public String surveyDetail(@PathVariable long surveyId, Model model) {
        SurveyDetailDto survey = surveyDao.findDetailById(surveyId);
        List<QuestionDto> questions = surveyDao.findQuestionsBySurveyId(surveyId);
        Map<Long, List<OptionDto>> optionMap = surveyDao.findOptionsBySurveyId(surveyId);

        // 追加：COMPLETEDのみ回答数
        int answerCount = adminResponseDao.countResponsesBySurveyId(surveyId, true);

        model.addAttribute("survey", survey);
        model.addAttribute("questions", questions);
        model.addAttribute("optionMap", optionMap);
        model.addAttribute("answerCount", answerCount);
        return "admin/survey-detail";
    }
}
