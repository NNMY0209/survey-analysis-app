package com.example.app.dao;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScaleImportDao {

    private final JdbcTemplate jdbcTemplate;

    public ScaleImportDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
        jdbcTemplate.update(
            "DELETE FROM scale_questions WHERE question_id = ?",
            questionId
        );
    }

    public void insert(long scaleId, long questionId, BigDecimal weight) {
        String sql = """
            INSERT INTO scale_questions (scale_id, question_id, weight)
            VALUES (?, ?, ?)
            """;
        jdbcTemplate.update(sql, scaleId, questionId, weight);
    }
}
