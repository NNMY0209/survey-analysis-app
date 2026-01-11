package com.example.app.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.OptionCountRowDto;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionAvgDto;
import com.example.app.dto.QuestionCountDto;
import com.example.app.dto.QuestionDto;
import com.example.app.dto.SurveyDetailDto;
import com.example.app.dto.SurveyDto;

@Repository
public class SurveyDao {

	private final JdbcTemplate jdbcTemplate;

	public SurveyDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// ===== アンケート一覧 =====
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

	// ===== アンケート詳細 =====
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

	// ===== 設問一覧 =====
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

	// ===== 選択肢 =====
	public Map<Long, List<OptionDto>> findOptionsBySurveyId(long surveyId) {

		String sql = """
				SELECT
				  q.question_id,
				  o.option_id,
				  o.option_text,
				  o.score,
				  o.display_order
				FROM questions q
				JOIN question_options o
				  ON q.question_id = o.question_id
				WHERE q.survey_id = ?
				ORDER BY q.question_id, o.display_order
				""";

		List<OptionDto> options = jdbcTemplate.query(sql, (rs, rowNum) -> {
			OptionDto o = new OptionDto();
			o.setQuestionId(rs.getLong("question_id"));
			o.setOptionId(rs.getLong("option_id"));
			o.setOptionText(rs.getString("option_text"));
			o.setScore(rs.getInt("score"));
			o.setDisplayOrder(rs.getInt("display_order"));
			return o;
		}, surveyId);

		// question_id ごとにまとめる
		Map<Long, List<OptionDto>> optionMap = new HashMap<>();

		for (OptionDto option : options) {
			optionMap
					.computeIfAbsent(option.getQuestionId(), k -> new ArrayList<>())
					.add(option);
		}

		return optionMap;
	}
	
	// ==== 設問ごとの回答数 ====
	public List<QuestionCountDto> findQuestionCountsBySurveyId(long surveyId) {
	    String sql = """
	            SELECT
	              q.question_id,
	              q.display_order,
	              q.question_text,
	              COUNT(a.answer_id) AS answer_count
	            FROM questions q
	            LEFT JOIN question_answers a
	              ON a.question_id = q.question_id
	            WHERE q.survey_id = ?
	            GROUP BY q.question_id, q.display_order, q.question_text
	            ORDER BY q.display_order, q.question_id
	            """;

	    return jdbcTemplate.query(sql, (rs, rowNum) -> {
	        QuestionCountDto dto = new QuestionCountDto();
	        dto.setQuestionId(rs.getLong("question_id"));
	        dto.setDisplayOrder(rs.getInt("display_order"));
	        dto.setQuestionText(rs.getString("question_text"));
	        dto.setAnswerCount(rs.getLong("answer_count"));
	        return dto;
	    }, surveyId);
	}

	// ==== 選択肢ごとの選択数 ====
	public List<OptionCountRowDto> findOptionCountsBySurveyId(long surveyId) {
	    String sql = """
	            SELECT
	              q.question_id,
	              q.display_order AS question_order,
	              q.question_text,
	              o.option_id,
	              o.display_order AS option_order,
	              o.option_text,
	              COUNT(a.answer_id) AS selected_count
	            FROM questions q
	            JOIN question_options o
	              ON o.question_id = q.question_id
	            LEFT JOIN question_answers a
	              ON a.option_id = o.option_id
	            WHERE q.survey_id = ?
	            GROUP BY
	              q.question_id, q.display_order, q.question_text,
	              o.option_id, o.display_order, o.option_text
	            ORDER BY
	              q.display_order, o.display_order
	            """;

	    return jdbcTemplate.query(sql, (rs, rowNum) -> {
	        OptionCountRowDto dto = new OptionCountRowDto();
	        dto.setQuestionId(rs.getLong("question_id"));
	        dto.setQuestionOrder(rs.getInt("question_order"));
	        dto.setQuestionText(rs.getString("question_text"));
	        dto.setOptionId(rs.getLong("option_id"));
	        dto.setOptionOrder(rs.getInt("option_order"));
	        dto.setOptionText(rs.getString("option_text"));
	        dto.setSelectedCount(rs.getLong("selected_count"));
	        return dto;
	    }, surveyId);
	}

	// ==== 設問ごとの平均スコア ====
	public List<QuestionAvgDto> findAvgScoresBySurveyId(long surveyId) {
	    String sql = """
	            SELECT
	              q.question_id,
	              q.display_order,
	              q.question_text,
	              AVG(o.score) AS avg_score
	            FROM questions q
	            JOIN question_answers a
	              ON a.question_id = q.question_id
	            JOIN question_options o
	              ON o.option_id = a.option_id
	            WHERE q.survey_id = ?
	              AND q.question_type = 'SINGLE'
	              AND o.score IS NOT NULL
	            GROUP BY q.question_id, q.display_order, q.question_text
	            ORDER BY q.display_order
	            """;

	    return jdbcTemplate.query(sql, (rs, rowNum) -> {
	        QuestionAvgDto dto = new QuestionAvgDto();
	        dto.setQuestionId(rs.getLong("question_id"));
	        dto.setDisplayOrder(rs.getInt("display_order"));
	        dto.setQuestionText(rs.getString("question_text"));
	        dto.setAvgScore(rs.getDouble("avg_score"));
	        return dto;
	    }, surveyId);
	}

}
