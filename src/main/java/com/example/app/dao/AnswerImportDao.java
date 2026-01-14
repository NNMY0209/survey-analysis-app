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

	/** respondents を作成（respondent_keyが入る前提） */
	public long createRespondent(long surveyId, String respondentKey) {
		String sql = "INSERT INTO respondents (survey_id, respondent_key, created_at) VALUES (?, ?, NOW())";
		jdbcTemplate.update(sql, surveyId, respondentKey);
		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/** response_sessions を作成（インポートはCOMPLETED扱い） */
	public long createCompletedSession(long surveyId, long respondentId) {
		String sql = """
				INSERT INTO response_sessions
				  (survey_id, respondent_id, status, started_at, completed_at, created_at, updated_at)
				VALUES (?, ?, 'COMPLETED', NOW(), NOW(), NOW(), NOW())
				""";
		jdbcTemplate.update(sql, surveyId, respondentId);
		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/** SINGLE：1行追加（answer_numberは1固定） */
	public void insertSingle(long responseId, long questionId, long optionId) {
		String sql = """
				INSERT INTO question_answers
				  (response_id, question_id, option_id, answer_number, answer_text, created_at)
				VALUES (?, ?, ?, 1, NULL, NOW())
				""";
		jdbcTemplate.update(sql, responseId, questionId, optionId);
	}

	/** MULTI：複数行追加（answer_number=1..n） */
	public void insertMulti(long responseId, long questionId, List<Long> optionIds) {
		String sql = """
				INSERT INTO question_answers
				  (response_id, question_id, option_id, answer_number, answer_text, created_at)
				VALUES (?, ?, ?, ?, NULL, NOW())
				""";
		int n = 1;
		for (Long optionId : optionIds) {
			jdbcTemplate.update(sql, responseId, questionId, optionId, n);
			n++;
		}
	}

	/** NUMBER/TEXT：answer_text に保存（option_idはNULL） */
	public void insertText(long responseId, long questionId, String answerText) {
		String sql = """
				INSERT INTO question_answers
				  (response_id, question_id, option_id, answer_number, answer_text, created_at)
				VALUES (?, ?, NULL, 1, ?, NOW())
				""";
		jdbcTemplate.update(sql, responseId, questionId, answerText);
	}
}
