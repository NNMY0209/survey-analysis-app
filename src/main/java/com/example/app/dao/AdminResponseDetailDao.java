package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.AdminResponseDetailRowDto;

@Repository
public class AdminResponseDetailDao {

	private final JdbcTemplate jdbcTemplate;

	public AdminResponseDetailDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<AdminResponseDetailRowDto> findDetailsByResponseId(long responseId) {

		String sql = """
								-- SINGLE/NUMBER/TEXT（qa.option_id を使う or text系）
				SELECT
				  qa.answer_id,
				  qa.response_id,
				  qa.question_id,
				  q.display_order AS question_order,
				  q.question_text,
				  q.question_type,
				  qa.option_id,
				  qo.option_text,
				  qa.answer_number,
				  qa.answer_text,
				  qa.created_at
				FROM question_answers qa
				JOIN questions q
				  ON q.question_id = qa.question_id
				LEFT JOIN question_options qo
				  ON qo.option_id = qa.option_id
				WHERE qa.response_id = ?
				  AND q.question_type <> 'MULTI'

				UNION ALL

				-- MULTI（中間テーブルから選択肢を展開）
				SELECT
				  qa.answer_id,
				  qa.response_id,
				  qa.question_id,
				  q.display_order AS question_order,
				  q.question_text,
				  q.question_type,
				  qam.option_id,
				  qo.option_text,
				  NULL AS answer_number,
				  NULL AS answer_text,
				  qa.created_at
				FROM question_answers qa
				JOIN questions q
				  ON q.question_id = qa.question_id
				JOIN question_answer_multi qam
				  ON qam.answer_id = qa.answer_id
				JOIN question_options qo
				  ON qo.option_id = qam.option_id
				WHERE qa.response_id = ?
				  AND q.question_type = 'MULTI'

				ORDER BY question_order, answer_id, option_id;

								""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			AdminResponseDetailRowDto dto = new AdminResponseDetailRowDto();
			dto.setAnswerId(rs.getLong("answer_id"));
			dto.setResponseId(rs.getLong("response_id"));

			dto.setQuestionId(rs.getLong("question_id"));
			dto.setQuestionOrder(rs.getInt("question_order"));
			dto.setQuestionText(rs.getString("question_text"));
			dto.setQuestionType(rs.getString("question_type"));

			dto.setOptionId(rs.getObject("option_id", Long.class));
			dto.setOptionText(rs.getString("option_text"));

			dto.setAnswerNumber(rs.getObject("answer_number", Integer.class));
			dto.setAnswerText(rs.getString("answer_text"));

			dto.setCreatedAt(rs.getTimestamp("created_at"));
			return dto;
		}, responseId, responseId);

	}
}
