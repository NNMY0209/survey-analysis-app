package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.QuestionDto;
import com.example.app.dto.SurveyDetailDto;
import com.example.app.dto.SurveyDto;

@Repository
public class SurveyDao {

	private final JdbcTemplate jdbcTemplate;

	public SurveyDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<SurveyDto> findAll() {

		String sql = """
				SELECT
				  survey_id,
				  title,
				  status,
				  open_at,
				  close_at
				FROM surveys
				ORDER BY survey_id DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			SurveyDto dto = new SurveyDto();
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setTitle(rs.getString("title"));
			dto.setStatus(rs.getString("status"));
			dto.setOpenAt(rs.getTimestamp("open_at"));
			dto.setCloseAt(rs.getTimestamp("close_at"));
			return dto;
		});
	}
	
	 public SurveyDetailDto findDetailById(long surveyId) {
	        String sql = """
	                SELECT survey_id, title, status, open_at, close_at
	                FROM surveys
	                WHERE survey_id = ?
	                """;

	        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
	            SurveyDetailDto dto = new SurveyDetailDto();
	            dto.setSurveyId(rs.getLong("survey_id"));
	            dto.setTitle(rs.getString("title"));
	            dto.setStatus(rs.getString("status"));
	            dto.setOpenAt(rs.getTimestamp("open_at"));
	            dto.setCloseAt(rs.getTimestamp("close_at"));
	            return dto;
	        }, surveyId);
	    }

	public List<QuestionDto> findQuestionsBySurveyId(long surveyId) {
		String sql = """
				SELECT question_id, survey_id, question_text, question_type, question_role,
				       is_reverse, is_required, display_order
				FROM questions
				WHERE survey_id = ?
				ORDER BY display_order ASC, question_id ASC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			QuestionDto q = new QuestionDto();
			q.setQuestionId(rs.getLong("question_id"));
			q.setSurveyId(rs.getLong("survey_id"));
			q.setQuestionText(rs.getString("question_text"));
			q.setQuestionType(rs.getString("question_type"));
			q.setQuestionRole(rs.getString("question_role"));
			q.setIsReverse(rs.getInt("is_reverse"));
			q.setIsRequired(rs.getInt("is_required"));
			q.setDisplayOrder(rs.getInt("display_order"));
			return q;
		}, surveyId);
	}
}
