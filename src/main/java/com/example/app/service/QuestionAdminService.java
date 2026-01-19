package com.example.app.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dao.QuestionAdminDao;
import com.example.app.dto.AdminQuestionCreateFormDto;
import com.example.app.dto.AdminQuestionCreateFormDto.OptionForm;

@Service
public class QuestionAdminService {

	private final QuestionAdminDao dao;
	private final AdminResponseDao adminResponseDao;

	public QuestionAdminService(QuestionAdminDao dao, AdminResponseDao adminResponseDao) {
		this.dao = dao;
		this.adminResponseDao = adminResponseDao;
	}

	@Transactional
	public void createQuestion(long surveyId, AdminQuestionCreateFormDto form) {

		// ===== 倫理ガード（回答が1件でもあれば変更禁止）=====
		if (adminResponseDao.existsAnyResponseBySurveyId(surveyId)) {
			throw new IllegalStateException("回答があるため設問を追加できません。");
		}

		// ===== 必須チェック =====
		if (form.getQuestionText() == null || form.getQuestionText().trim().isEmpty()) {
			throw new IllegalArgumentException("設問文を入力してください。");
		}

		// ===== 表示順：常に自動採番（UI入力は無視）=====
		int displayOrder = dao.getNextDisplayOrder(surveyId);

		// ===== 選択肢：テンプレに応じて整形 =====
		List<OptionForm> normalized = normalizeOptions(form);

		if (needsOptions(form.getQuestionType()) && normalized.size() < 2) {
			throw new IllegalArgumentException("選択肢は2件以上入力してください。");
		}

		// ===== questions insert =====
		long questionId = dao.insertQuestion(surveyId, form, displayOrder);

		// ===== options insert（選択肢が必要なタイプのみ）=====
		if (needsOptions(form.getQuestionType())) {
			int optOrder = 1;
			for (OptionForm opt : normalized) {
				dao.insertOption(questionId, opt, optOrder++);
			}
		}
	}

	/**
	 * 選択肢が必要かどうか
	 * - SINGLE_CHOICE / MULTI_CHOICE のみ options を保存
	 */
	private boolean needsOptions(String questionType) {
		if (questionType == null)
			return true;
		return "SINGLE_CHOICE".equals(questionType) || "MULTI_CHOICE".equals(questionType);
	}

	/**
	 * options を
	 * - 空行除去
	 * - score 自動付与（LIKERT5/7） or CUSTOM時は必須
	 * - correct は ATTENTION_CHECK のときだけ有効（それ以外は false）
	 * に整形して返す
	 */
	private List<OptionForm> normalizeOptions(AdminQuestionCreateFormDto form) {

		List<OptionForm> result = new ArrayList<>();

		String template = form.getOptionTemplate();
		if (template == null || template.isBlank())
			template = "LIKERT5";

		boolean attention = "ATTENTION_CHECK".equals(form.getQuestionRole());

		int autoScore = 1;

		if (form.getOptions() == null)
			return result;

		for (OptionForm opt : form.getOptions()) {
			if (opt == null)
				continue;

			String text = (opt.getOptionText() == null) ? "" : opt.getOptionText().trim();
			if (text.isEmpty())
				continue; // 空行は捨てる

			OptionForm copy = new OptionForm();
			copy.setOptionText(text);

			// score
			if ("CUSTOM".equals(template)) {
				if (opt.getScore() == null) {
					throw new IllegalArgumentException("カスタム選択肢ではスコアが必須です。");
				}
				copy.setScore(opt.getScore());
			} else {
				// LIKERT5/7 は入力された行数に応じて 1..N を採番
				copy.setScore(autoScore++);
			}

			// correct（注意確認のみ意味を持つ）
			copy.setCorrect(attention && opt.isCorrect());

			result.add(copy);
		}

		return result;
	}
}
