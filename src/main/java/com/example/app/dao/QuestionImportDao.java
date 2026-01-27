package com.example.app.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionImportDao {

	private final JdbcTemplate jdbcTemplate;

	public QuestionImportDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/** (survey_id, display_order) で question_id を引く */
	public Optional<Long> findQuestionId(long surveyId, int displayOrder) {
		String sql = """
				SELECT question_id
				  FROM questions
				 WHERE survey_id = ?
				   AND display_order = ?
				""";
		var list = jdbcTemplate.query(sql, (rs, n) -> rs.getLong("question_id"), surveyId, displayOrder);
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

	public long insertQuestion(
			long surveyId,
			int displayOrder,
			String text,
			String type,
			String role,
			boolean reverse,
			boolean required) {
		String sql = """
				INSERT INTO questions
				  (survey_id, question_text, question_type, question_role, is_reverse, is_required, display_order)
				VALUES
				  (?, ?, ?, ?, ?, ?, ?)
				""";

		KeyHolder kh = new GeneratedKeyHolder();
		jdbcTemplate.update(con -> {
			PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, surveyId);
			ps.setString(2, text);
			ps.setString(3, type);
			ps.setString(4, role);
			ps.setBoolean(5, reverse);
			ps.setBoolean(6, required);
			ps.setInt(7, displayOrder);
			return ps;
		}, kh);

		Number key = kh.getKey();
		if (key == null)
			throw new IllegalStateException("Failed to get generated question_id");
		return key.longValue();
	}

	public void updateQuestion(
			long questionId,
			String text,
			String type,
			String role,
			boolean reverse,
			boolean required) {
		String sql = """
				UPDATE questions
				   SET question_text = ?,
				       question_type = ?,
				       question_role = ?,
				       is_reverse = ?,
				       is_required = ?
				 WHERE question_id = ?
				""";
		jdbcTemplate.update(sql, text, type, role, reverse, required, questionId);
	}

	/** options 全置換用 */
	public void deleteOptionsByQuestionId(long questionId) {
		jdbcTemplate.update("DELETE FROM question_options WHERE question_id = ?", questionId);
	}

	public void insertOption(
			long questionId,
			int displayOrder,
			String optionText,
			int score,
			boolean correct) {
		String sql = """
				INSERT INTO question_options
				  (question_id, option_text, score, is_correct, display_order)
				VALUES
				  (?, ?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql, questionId, optionText, score, correct, displayOrder);
	}

	/** (survey_id) で display_order -> question_id のMapを返す */
	public Map<Integer, Long> findQuestionIdByOrder(long surveyId) {
		String sql = """
				SELECT display_order, question_id
				  FROM questions
				 WHERE survey_id = ?
				""";
		return jdbcTemplate.query(sql, rs -> {
			Map<Integer, Long> map = new HashMap<>();
			while (rs.next()) {
				map.put(rs.getInt("display_order"), rs.getLong("question_id"));
			}
			return map;
		}, surveyId);
	}

	/** (survey_id) で display_order -> question_type のMapを返す（options反映時の安全用） */
	public Map<Integer, String> findQuestionTypeByOrder(long surveyId) {
		String sql = """
				SELECT display_order, question_type
				  FROM questions
				 WHERE survey_id = ?
				""";
		return jdbcTemplate.query(sql, rs -> {
			Map<Integer, String> map = new HashMap<>();
			while (rs.next()) {
				map.put(rs.getInt("display_order"), rs.getString("question_type"));
			}
			return map;
		}, surveyId);
	}

}
