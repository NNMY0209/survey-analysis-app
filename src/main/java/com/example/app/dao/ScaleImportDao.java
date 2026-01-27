package com.example.app.dao;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScaleImportDao {

    private final JdbcTemplate jdbcTemplate;

    public ScaleImportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ===== 既存（残す） =====
    public Optional<Long> findScaleId(long surveyId, String scaleCode) {
        String sql = """
            SELECT scale_id
              FROM scales
             WHERE survey_id = ?
               AND scale_code = ?
            """;
        var list = jdbcTemplate.query(sql, (rs, n) -> rs.getLong("scale_id"), surveyId, scaleCode);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public void deleteByQuestionId(long questionId) {
        jdbcTemplate.update("DELETE FROM scale_questions WHERE question_id = ?", questionId);
    }

    public void insert(long scaleId, long questionId, BigDecimal weight) {
        String sql = """
            INSERT INTO scale_questions (scale_id, question_id, weight)
            VALUES (?, ?, ?)
            """;
        jdbcTemplate.update(sql, scaleId, questionId, weight);
    }

    // ===== A方式（2CSV）用に追加 =====

    /** scales.csv: (survey_id, scale_code) をキーにUPSERT */
    public void upsertScale(long surveyId, String scaleCode, String scaleName, String description) {
        String sql = """
            INSERT INTO scales (survey_id, scale_code, scale_name, description)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              scale_name = VALUES(scale_name),
              description = VALUES(description)
            """;
        jdbcTemplate.update(sql, surveyId, scaleCode, scaleName, description);
    }

    /** survey内の scale_code -> scale_id */
    public Map<String, Long> findScaleIdByCode(long surveyId) {
        String sql = """
            SELECT scale_code, scale_id
              FROM scales
             WHERE survey_id = ?
             ORDER BY scale_id
            """;
        List<Entry<String, Long>> rows = jdbcTemplate.query(sql,
                (rs, n) -> Map.entry(rs.getString("scale_code"), rs.getLong("scale_id")),
                surveyId);

        Map<String, Long> map = new LinkedHashMap<>();
        for (var e : rows) map.put(e.getKey(), e.getValue());
        return map;
    }

    /** survey内の display_order -> question_id */
    public Map<Integer, Long> findQuestionIdByOrder(long surveyId) {
        String sql = """
            SELECT display_order, question_id
              FROM questions
             WHERE survey_id = ?
             ORDER BY display_order
            """;
        List<Entry<Integer, Long>> rows = jdbcTemplate.query(sql,
                (rs, n) -> Map.entry(rs.getInt("display_order"), rs.getLong("question_id")),
                surveyId);

        Map<Integer, Long> map = new LinkedHashMap<>();
        for (var e : rows) map.put(e.getKey(), e.getValue());
        return map;
    }

    /** 全置換用：survey内の scale_questions を削除 */
    public int deleteScaleQuestionsBySurveyId(long surveyId) {
        String sql = """
            DELETE sq
              FROM scale_questions sq
              JOIN scales s ON s.scale_id = sq.scale_id
             WHERE s.survey_id = ?
            """;
        return jdbcTemplate.update(sql, surveyId);
    }
}
