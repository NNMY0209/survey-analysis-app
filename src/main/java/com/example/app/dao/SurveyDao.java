package com.example.app.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.app.dto.AdminSurveyRowDto;
import com.example.app.dto.OptionCountRowDto;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionAvgDto;
import com.example.app.dto.QuestionCountDto;
import com.example.app.dto.QuestionDto;
import com.example.app.dto.ScaleAvgDto;
import com.example.app.dto.SurveyConsentDto;
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
				SELECT survey_id, title, description, consent_text, status, open_at, close_at
				FROM surveys
				WHERE survey_id = ?
				""";

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
			SurveyDetailDto dto = new SurveyDetailDto();
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setTitle(rs.getString("title"));
			dto.setDescription(rs.getString("description"));
			dto.setConsentText(rs.getString("consent_text"));
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

	// ==== 設問ごとの回答数（COMPLETEDのみ）====
	public List<QuestionCountDto> findQuestionCountsBySurveyId(long surveyId) {
		String sql = """
				SELECT
				  q.question_id,
				  q.display_order,
				  q.question_text,
				  COUNT(DISTINCT rs.response_id) AS answer_count
				FROM questions q
				LEFT JOIN respondents r
				  ON r.survey_id = q.survey_id
				LEFT JOIN response_sessions rs
				  ON rs.respondent_id = r.respondent_id
				 AND rs.status = 'COMPLETED'
				LEFT JOIN question_answers a
				  ON a.response_id = rs.response_id
				 AND a.question_id = q.question_id
				WHERE q.survey_id = ?
				  AND a.answer_id IS NOT NULL
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

	// ==== 選択肢ごとの選択数（MULTI対応・COMPLETEDのみ）====
	public List<OptionCountRowDto> findOptionCountsBySurveyId(long surveyId) {

		String sql = """
				SELECT
				  q.question_id,
				  q.display_order AS question_order,
				  q.question_text,
				  o.option_id,
				  o.display_order AS option_order,
				  o.option_text,
				  COUNT(x.selected_option_id) AS selected_count
				FROM questions q
				JOIN question_options o
				  ON o.question_id = q.question_id
				LEFT JOIN (
				  -- SINGLE: question_answers.option_id
				  SELECT
				    qa.question_id,
				    qa.option_id AS selected_option_id
				  FROM response_sessions rs
				  JOIN respondents r ON r.respondent_id = rs.respondent_id
				  JOIN question_answers qa ON qa.response_id = rs.response_id
				  JOIN questions q2 ON q2.question_id = qa.question_id
				  WHERE r.survey_id = ?
				    AND rs.status = 'COMPLETED'
				    AND qa.option_id IS NOT NULL

				  UNION ALL

				  -- MULTI: question_answer_multi.option_id
				  SELECT
				    qa.question_id,
				    qam.option_id AS selected_option_id
				  FROM response_sessions rs
				  JOIN respondents r ON r.respondent_id = rs.respondent_id
				  JOIN question_answers qa ON qa.response_id = rs.response_id
				  JOIN question_answer_multi qam ON qam.answer_id = qa.answer_id
				  JOIN questions q2 ON q2.question_id = qa.question_id
				  WHERE r.survey_id = ?
				    AND rs.status = 'COMPLETED'
				) x
				  ON x.question_id = q.question_id
				 AND x.selected_option_id = o.option_id
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
		}, surveyId, surveyId, surveyId);
	}

	// ==== 設問ごとの平均スコア（SINGLE・COMPLETEDのみ）====
	public List<QuestionAvgDto> findAvgScoresBySurveyId(long surveyId) {
		String sql = """
				SELECT
				  q.question_id,
				  q.display_order,
				  q.question_text,
				  AVG(o.score) AS avg_score
				FROM questions q
				JOIN respondents r
				  ON r.survey_id = q.survey_id
				JOIN response_sessions rs
				  ON rs.respondent_id = r.respondent_id
				 AND rs.status = 'COMPLETED'
				JOIN question_answers a
				  ON a.response_id = rs.response_id
				 AND a.question_id = q.question_id
				JOIN question_options o
				  ON o.option_id = a.option_id
				WHERE q.survey_id = ?
				  AND q.question_type = 'SINGLE_CHOICE'
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

	// ===== アンケートの回答数取得 =====
	public List<AdminSurveyRowDto> findAllWithAnswerCount() {
		String sql = """
				SELECT
				  s.survey_id,
				  s.title,
				  s.status,
				  s.open_at,
				  s.close_at,
				  COUNT(rs.response_id) AS answer_count
				FROM surveys s
				LEFT JOIN respondents r
				  ON r.survey_id = s.survey_id
				LEFT JOIN response_sessions rs
				  ON rs.respondent_id = r.respondent_id
				 AND rs.status = 'COMPLETED'
				GROUP BY
				  s.survey_id, s.title, s.status, s.open_at, s.close_at
				ORDER BY s.survey_id DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			AdminSurveyRowDto dto = new AdminSurveyRowDto();
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setTitle(rs.getString("title"));
			dto.setStatus(rs.getString("status"));
			dto.setOpenAt(rs.getTimestamp("open_at"));
			dto.setCloseAt(rs.getTimestamp("close_at"));
			dto.setAnswerCount(rs.getInt("answer_count"));
			return dto;
		});
	}

	// ==== 下位尺度ごとの平均（SINGLE / COMPLETEDのみ）====
	public List<ScaleAvgDto> findScaleAveragesBySurveyId(long surveyId) {

		String sql = """
				WITH scored AS (
				  SELECT
				    s.scale_id,
				    s.scale_code,
				    s.scale_name,
				    r.respondent_id,
				    (
				      CASE
				        WHEN q.is_reverse = 1 THEN (qstat.max_score + qstat.min_score - o.score)
				        ELSE o.score
				      END
				    ) * sq.weight AS wscore,
				    sq.weight AS w
				  FROM scales s
				  JOIN scale_questions sq ON sq.scale_id = s.scale_id
				  JOIN questions q ON q.question_id = sq.question_id
				  JOIN question_answers a ON a.question_id = q.question_id
				  JOIN question_options o ON o.option_id = a.option_id
				  JOIN response_sessions rs ON rs.response_id = a.response_id
				  JOIN respondents r ON r.respondent_id = rs.respondent_id
				  JOIN (
				    SELECT question_id, MIN(score) AS min_score, MAX(score) AS max_score
				    FROM question_options
				    GROUP BY question_id
				  ) qstat ON qstat.question_id = q.question_id
				  WHERE s.survey_id = ?
				    AND rs.status = 'COMPLETED'
				    AND q.question_type = 'SINGLE_CHOICE'
				),
				per_resp AS (
				  SELECT
				    scale_id,
				    scale_code,
				    scale_name,
				    respondent_id,
				    SUM(wscore) / NULLIF(SUM(w), 0) AS scale_score
				  FROM scored
				  GROUP BY scale_id, scale_code, scale_name, respondent_id
				),
				ranked AS (
				  SELECT
				    scale_id,
				    scale_code,
				    scale_name,
				    respondent_id,
				    scale_score,
				    ROW_NUMBER() OVER (PARTITION BY scale_id ORDER BY scale_score) AS rn,
				    COUNT(*) OVER (PARTITION BY scale_id) AS cnt
				  FROM per_resp
				)
				SELECT
				  scale_id,
				  scale_code,
				  scale_name,
				  COUNT(*) AS respondent_count,
				  AVG(scale_score) AS avg_score,
				  STDDEV_SAMP(scale_score) AS sd_score,
				  AVG(CASE
				        WHEN rn IN ((cnt + 1) DIV 2, (cnt + 2) DIV 2)
				        THEN scale_score
				      END) AS median_score
				FROM ranked
				GROUP BY scale_id, scale_code, scale_name
				ORDER BY scale_id
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ScaleAvgDto dto = new ScaleAvgDto();
			dto.setScaleId(rs.getLong("scale_id"));
			dto.setScaleCode(rs.getString("scale_code"));
			dto.setScaleName(rs.getString("scale_name"));
			dto.setRespondentCount(rs.getLong("respondent_count"));
			dto.setAvgScore(rs.getDouble("avg_score"));
			dto.setSdScore(rs.getDouble("sd_score"));
			dto.setMedianScore(rs.getDouble("median_score"));

			return dto;
		}, surveyId);
	}

	// ===== 公開設定（回答アクセス判定用）=====
	public SurveyDetailDto findPublishSettingsById(long surveyId) {
		String sql = """
				SELECT survey_id, status, open_at, close_at
				FROM surveys
				WHERE survey_id = ?
				""";

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
			SurveyDetailDto dto = new SurveyDetailDto();
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setStatus(rs.getString("status"));
			dto.setOpenAt(rs.getTimestamp("open_at"));
			dto.setCloseAt(rs.getTimestamp("close_at"));
			return dto;
		}, surveyId);
	}

	// ===== 公開設定 更新（管理者）=====
	public int updatePublishSettings(long surveyId, String status,
			java.sql.Timestamp openAt, java.sql.Timestamp closeAt,
			long updatedBy) {

		String sql = """
				UPDATE surveys
				SET status = ?, open_at = ?, close_at = ?, updated_by = ?
				WHERE survey_id = ?
				""";

		return jdbcTemplate.update(sql, status, openAt, closeAt, updatedBy, surveyId);
	}

	// ===== 同意画面用（説明文・同意文）=====
	public SurveyConsentDto findConsentById(long surveyId) {
		String sql = """
				SELECT survey_id, title, description, consent_text, status, open_at, close_at
				FROM surveys
				WHERE survey_id = ?
				""";

		return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
			SurveyConsentDto dto = new SurveyConsentDto();
			dto.setSurveyId(rs.getLong("survey_id"));
			dto.setTitle(rs.getString("title"));
			dto.setDescription(rs.getString("description"));
			dto.setConsentText(rs.getString("consent_text"));
			dto.setStatus(rs.getString("status"));
			dto.setOpenAt(rs.getTimestamp("open_at"));
			dto.setCloseAt(rs.getTimestamp("close_at"));
			return dto;
		}, surveyId);
	}

	// ===== 説明文・同意文 更新（管理者）=====
	public int updateDescriptionAndConsent(long surveyId,
			String description, String consentText,
			long updatedBy) {

		String sql = """
				UPDATE surveys
				SET description = ?, consent_text = ?, updated_by = ?
				WHERE survey_id = ?
				""";

		return jdbcTemplate.update(sql, description, consentText, updatedBy, surveyId);
	}

	public long insertDraft(String title, long adminUserId) {
		String sql = """
				INSERT INTO surveys (title, status, created_by, updated_by)
				VALUES (?, 'DRAFT', ?, ?)
				""";
		KeyHolder kh = new GeneratedKeyHolder();
		jdbcTemplate.update(con -> {
			PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, title);
			ps.setLong(2, adminUserId);
			ps.setLong(3, adminUserId);
			return ps;
		}, kh);

		Number key = kh.getKey();
		if (key == null)
			throw new IllegalStateException("Failed to get generated survey_id");
		return key.longValue();
	}

}
