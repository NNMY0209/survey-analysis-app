package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.ImportOptionMetaDto;
import com.example.app.dto.ImportQuestionMetaDto;

@Repository
public class ImportMetaDao {

	private final JdbcTemplate jdbcTemplate;

	public ImportMetaDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ImportQuestionMetaDto> findQuestions(long surveyId) {
		String sql = """
				SELECT question_id, question_text, question_type, display_order
				FROM questions
				WHERE survey_id = ?
				ORDER BY display_order
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ImportQuestionMetaDto dto = new ImportQuestionMetaDto();
			dto.setQuestionId(rs.getLong("question_id"));
			dto.setQuestionText(rs.getString("question_text"));
			dto.setQuestionType(rs.getString("question_type"));
			dto.setDisplayOrder(rs.getInt("display_order"));
			return dto;
		}, surveyId);
	}

	public List<ImportOptionMetaDto> findOptions(long surveyId) {
		String sql = """
				SELECT qo.question_id, qo.option_id, qo.option_text, qo.display_order
				FROM question_options qo
				JOIN questions q ON q.question_id = qo.question_id
				WHERE q.survey_id = ?
				ORDER BY qo.question_id, qo.display_order
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ImportOptionMetaDto dto = new ImportOptionMetaDto();
			dto.setQuestionId(rs.getLong("question_id"));
			dto.setOptionId(rs.getLong("option_id"));
			dto.setOptionText(rs.getString("option_text"));
			dto.setDisplayOrder(rs.getInt("display_order"));
			return dto;
		}, surveyId);
	}
}
