package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnswerImportDao {

	private final JdbcTemplate jdbcTemplate;

	public AnswerImportDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * respondents を作成/取得（survey_id + respondent_key 一意）
	 * - 新規ならINSERT
	 * - 既存なら既存行のIDを返す（再インポートに強い）
	 */
	public long upsertRespondent(long surveyId, String respondentKey) {
		String sql = """
				INSERT INTO respondents (survey_id, respondent_key, created_at)
				VALUES (?, ?, NOW())
				ON DUPLICATE KEY UPDATE respondent_id = LAST_INSERT_ID(respondent_id)
				""";
		jdbcTemplate.update(sql, surveyId, respondentKey);
		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/**
	 * response_sessions を作成（インポートはCOMPLETED扱い）
	 * ※ response_sessions に survey_id 列は無い
	 */
	public long createCompletedSession(long respondentId) {
		String sql = """
				INSERT INTO response_sessions
				  (respondent_id, status, started_at, completed_at, created_at, updated_at)
				VALUES (?, 'COMPLETED', NOW(), NOW(), NOW(), NOW())
				""";
		jdbcTemplate.update(sql, respondentId);
		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/**
	 * question_answers の「親行」を作成/取得（response_id + question_id 一意）
	 * - SINGLE/NUMBER/TEXT/MULTI いずれもまずここを確保する
	 */
	private long upsertAnswerRow(long responseId, long questionId) {
		// UNIQUE(response_id, question_id) 前提で、既存があればIDを取ってくる
		String sql = """
				INSERT INTO question_answers
				  (response_id, question_id, option_id, answer_number, answer_text, created_at)
				VALUES (?, ?, NULL, NULL, NULL, NOW())
				ON DUPLICATE KEY UPDATE answer_id = LAST_INSERT_ID(answer_id)
				""";
		jdbcTemplate.update(sql, responseId, questionId);
		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/** SINGLE：option_id を親行に保存（MULTIとは排他） */
	public void upsertSingle(long responseId, long questionId, long optionId) {
		// 親行を確保してから更新
		upsertAnswerRow(responseId, questionId);
		String sql = """
				UPDATE question_answers
				   SET option_id = ?,
				       answer_number = NULL,
				       answer_text = NULL
				 WHERE response_id = ? AND question_id = ?
				""";
		jdbcTemplate.update(sql, optionId, responseId, questionId);
	}

	/** NUMBER/TEXT：answer_text に保存（必要ならanswer_numberに数値を入れる設計も可） */
	public void upsertText(long responseId, long questionId, String answerText) {
		upsertAnswerRow(responseId, questionId);
		String sql = """
				UPDATE question_answers
				   SET option_id = NULL,
				       answer_number = 1,
				       answer_text = ?
				 WHERE response_id = ? AND question_id = ?
				""";
		jdbcTemplate.update(sql, answerText, responseId, questionId);
	}

	/**
	 * MULTI：中間テーブル question_answer_multi に保存
	 * - 親行（question_answers）は 1行だけ確保
	 * - 既存の選択肢は削除して入れ直し（再インポート時に安全）
	 */
	public void upsertMulti(long responseId, long questionId, List<Long> optionIds) {
		long answerId = upsertAnswerRow(responseId, questionId);

		// 親行側は MULTIとして option_id/answer_text をクリアしておく（任意だけど事故防止）
		jdbcTemplate.update("""
				UPDATE question_answers
				   SET option_id = NULL,
				       answer_number = NULL,
				       answer_text = NULL
				 WHERE answer_id = ?
				""", answerId);

		// 既存の選択肢を消して入れ直し
		jdbcTemplate.update("DELETE FROM question_answer_multi WHERE answer_id = ?", answerId);

		String sql = "INSERT INTO question_answer_multi (answer_id, option_id, created_at) VALUES (?, ?, NOW())";
		for (Long optionId : optionIds) {
			jdbcTemplate.update(sql, answerId, optionId);
		}
	}
}
