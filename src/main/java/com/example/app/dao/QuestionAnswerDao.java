package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionAnswerDao {

	private final JdbcTemplate jdbcTemplate;

	public QuestionAnswerDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void deleteByResponseId(long responseId) {
		jdbcTemplate.update("DELETE FROM question_answers WHERE response_id = ?", responseId);
	}

	public void insertSingle(long responseId, long questionId, long optionId) {
		jdbcTemplate.update("""
			INSERT INTO question_answers
			(response_id, question_id, option_id, answer_number, answer_text, created_at)
			VALUES (?, ?, ?, 1, NULL, NOW())
		""", responseId, questionId, optionId);
	}

	public void insertMulti(long responseId, long questionId, List<Long> optionIds) {
		String sql = """
			INSERT INTO question_answers
			(response_id, question_id, option_id, answer_number, answer_text, created_at)
			VALUES (?, ?, ?, ?, NULL, NOW())
		""";

		jdbcTemplate.batchUpdate(sql, optionIds, 200, (ps, optionId) -> {
			int idx = optionIds.indexOf(optionId) + 1; // 1..n
			ps.setLong(1, responseId);
			ps.setLong(2, questionId);
			ps.setLong(3, optionId);
			ps.setInt(4, idx);
		});
	}

	public void insertText(long responseId, long questionId, String answerText) {
		jdbcTemplate.update(con -> {
			var ps = con.prepareStatement("""
				INSERT INTO question_answers
				(response_id, question_id, option_id, answer_number, answer_text, created_at)
				VALUES (?, ?, NULL, 1, ?, NOW())
			""");
			ps.setLong(1, responseId);
			ps.setLong(2, questionId);
			ps.setString(3, answerText);
			return ps;
		});
	}
}
