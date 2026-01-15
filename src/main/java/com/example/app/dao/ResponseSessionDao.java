package com.example.app.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResponseSessionDao {

	private final JdbcTemplate jdbcTemplate;

	public ResponseSessionDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** respondents を作成して respondent_id を返す（respondent_keyはUUID） */
	public long createRespondent(long surveyId) {
		jdbcTemplate.update("""
				    INSERT INTO respondents (survey_id, respondent_key, created_at)
				    VALUES (?, UUID(), NOW())
				""", surveyId);

		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return (id == null) ? 0L : id;
	}

	/** response_sessions を作成して response_id を返す（※survey_id列は無い） */
	public long createResponseSession(long respondentId) {
		jdbcTemplate.update("""
				    INSERT INTO response_sessions (respondent_id, status, started_at, created_at, updated_at)
				    VALUES (?, 'IN_PROGRESS', NOW(), NOW(), NOW())
				""", respondentId);

		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return (id == null) ? 0L : id;
	}

	public void markCompleted(long responseId) {
		jdbcTemplate.update("""
				    UPDATE response_sessions
				       SET status = 'COMPLETED',
				           completed_at = NOW(),
				           updated_at = NOW()
				     WHERE response_id = ?
				""", responseId);
	}

	/** responseId が指定 surveyId に属するかチェック（respondents経由） */
	public void assertBelongsToSurvey(long responseId, long surveyId) {
		Integer count = jdbcTemplate.queryForObject("""
				    SELECT COUNT(*)
				      FROM response_sessions rs
				      JOIN respondents r ON r.respondent_id = rs.respondent_id
				     WHERE rs.response_id = ?
				       AND r.survey_id = ?
				""", Integer.class, responseId, surveyId);

		if (count == null || count == 0) {
			throw new IllegalArgumentException("Invalid response_id or survey_id");
		}
	}
}
