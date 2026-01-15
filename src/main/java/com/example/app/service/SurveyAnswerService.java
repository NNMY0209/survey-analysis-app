package com.example.app.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.dao.QuestionAnswerDao;
import com.example.app.dao.ResponseSessionDao;
import com.example.app.dao.SurveyDao;
import com.example.app.dto.AnswerItemDto;
import com.example.app.dto.AnswerQuestionDto;
import com.example.app.dto.AnswerSubmitRequestDto;
import com.example.app.dto.AnswerViewModel;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionDto;

@Service
public class SurveyAnswerService {

	private final SurveyDao surveyDao;
	private final ResponseSessionDao responseSessionDao;
	private final QuestionAnswerDao questionAnswerDao;

	public SurveyAnswerService(
			SurveyDao surveyDao,
			ResponseSessionDao responseSessionDao,
			QuestionAnswerDao questionAnswerDao) {
		this.surveyDao = surveyDao;
		this.responseSessionDao = responseSessionDao;
		this.questionAnswerDao = questionAnswerDao;
	}

	/** 回答画面用VM作成（respondent + response_session を作り、設問＋選択肢を組み立てる） */
	public AnswerViewModel buildAnswerView(long surveyId) {
		long respondentId = responseSessionDao.createRespondent(surveyId);
		long responseId = responseSessionDao.createResponseSession(respondentId);

		List<QuestionDto> questions = surveyDao.findQuestionsBySurveyId(surveyId);
		Map<Long, List<OptionDto>> optionMap = surveyDao.findOptionsBySurveyId(surveyId);

		List<AnswerQuestionDto> viewQuestions = new ArrayList<>();
		for (QuestionDto q : questions) {
			AnswerQuestionDto vq = new AnswerQuestionDto();
			vq.setQuestionId(q.getQuestionId());
			vq.setQuestionText(q.getQuestionText());
			vq.setQuestionType(q.getQuestionType());
			vq.setOptions(optionMap.getOrDefault(q.getQuestionId(), List.of()));
			viewQuestions.add(vq);
		}

		AnswerViewModel vm = new AnswerViewModel();
		vm.setSurveyId(surveyId);
		vm.setResponseId(responseId);
		vm.setQuestions(viewQuestions);
		return vm;
	}

	/** 送信内容をDBへ保存（SINGLE/MULTI/TEXT） */
	@Transactional
	public void submit(AnswerSubmitRequestDto req) {

		// ざっくり整合性チェック（最低限）
		responseSessionDao.assertBelongsToSurvey(req.getResponseId(), req.getSurveyId());

		// 編集・再送信に強い：一旦消して入れ直し
		questionAnswerDao.deleteByResponseId(req.getResponseId());

		if (req.getAnswers() != null) {
			for (AnswerItemDto a : req.getAnswers()) {
				if (a == null)
					continue;

				String type = a.getQuestionType();
				if ("SINGLE".equals(type)) {
					if (a.getOptionId() == null)
						continue;
					questionAnswerDao.insertSingle(req.getResponseId(), a.getQuestionId(), a.getOptionId());

				} else if ("MULTI".equals(type)) {
					if (a.getOptionIds() == null || a.getOptionIds().isEmpty())
						continue;
					questionAnswerDao.insertMulti(req.getResponseId(), a.getQuestionId(), a.getOptionIds());

				} else if ("TEXT".equals(type) || "NUMBER".equals(type)) {
					String text = a.getAnswerText();
					if (text == null)
						text = "";
					questionAnswerDao.insertText(req.getResponseId(), a.getQuestionId(), text);
				}

			}
		}

		responseSessionDao.markCompleted(req.getResponseId());
	}
}
