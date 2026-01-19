package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.AdminResponseRowDto;

@Repository
public class AdminResponseDao {

	private final JdbcTemplate jdbcTemplate;

	public AdminResponseDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 回答一覧（survey_id指定）
	 * - surveys.titleも表示
	 * - respondents.respondent_keyも表示
	 * - question_answers件数も表示
	 */
	public List<AdminResponseRowDto> findResponseList(long surveyId, boolean completedOnly) {

		String sql = """
				SELECT
				  rs.response_id,
				  r.survey_id,
				  s.title AS survey_title,
				  rs.respondent_id,
				  r.respondent_key,
				  rs.status,
				  rs.started_at,
				  rs.completed_at,
				  COUNT(qa.answer_id) AS answer_count
				FROM response_sessions rs
				JOIN respondents r
				  ON r.respondent_id = rs.respondent_id
				JOIN surveys s
				  ON s.survey_id = r.survey_id
				LEFT JOIN question_answers qa
				  ON qa.response_id = rs.response_id
				WHERE r.survey_id = ?
				""" + (completedOnly ? " AND rs.status = 'COMPLETED' " : "") + """
				GROUP BY
				  rs.response_id, r.survey_id, s.title,
				  rs.respondent_id, r.respondent_key,
				  rs.status, rs.started_at, rs.completed_at
				ORDER BY rs.response_id DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			AdminResponseRowDto dto = new AdminResponseRowDto();
			dto.setResponseId(rs.getLong("response_id"));
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setSurveyTitle(rs.getString("survey_title"));

			dto.setRespondentId(rs.getLong("respondent_id"));
			dto.setRespondentKey(rs.getString("respondent_key"));

			dto.setStatus(rs.getString("status"));
			dto.setStartedAt(rs.getTimestamp("started_at"));
			dto.setCompletedAt(rs.getTimestamp("completed_at"));

			dto.setAnswerCount(rs.getInt("answer_count"));
			return dto;
		}, surveyId);
	}

	public int countResponsesBySurveyId(long surveyId, boolean completedOnly) {
		String sql = """
				SELECT COUNT(*)
				  FROM response_sessions rs
				  JOIN respondents r ON r.respondent_id = rs.respondent_id
				 WHERE r.survey_id = ?
				""" + (completedOnly ? " AND rs.status = 'COMPLETED' " : "");

		Integer n = jdbcTemplate.queryForObject(sql, Integer.class, surveyId);
		return (n == null) ? 0 : n;
	}
	
	public boolean existsAnyResponseBySurveyId(long surveyId) {
	    String sql = """
	        SELECT 1
	          FROM response_sessions rs
	          JOIN respondents r ON r.respondent_id = rs.respondent_id
	         WHERE r.survey_id = ?
	         LIMIT 1
	        """;
	    List<Integer> list = jdbcTemplate.query(sql, (rs, rowNum) -> 1, surveyId);
	    return !list.isEmpty();
	}


}
