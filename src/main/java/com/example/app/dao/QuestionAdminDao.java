package com.example.app.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.example.app.dto.AdminQuestionCreateFormDto;

@Repository
public class QuestionAdminDao {
    private final JdbcTemplate jdbcTemplate;

    public QuestionAdminDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getNextDisplayOrder(long surveyId) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(display_order), 0) FROM questions WHERE survey_id = ?",
                Integer.class,
                surveyId
        );
        return (max == null ? 1 : max + 1);
    }

    public long insertQuestion(long surveyId, AdminQuestionCreateFormDto form, int displayOrder) {
        String sql = """
            INSERT INTO questions
              (survey_id, question_text, question_type, question_role, is_reverse, is_required, display_order)
            VALUES
              (?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, surveyId);
            ps.setString(2, form.getQuestionText());
            ps.setString(3, form.getQuestionType());
            ps.setString(4, form.getQuestionRole());
            ps.setBoolean(5, form.isReverse());
            ps.setBoolean(6, form.isRequired());
            ps.setInt(7, displayOrder);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("Failed to get generated question_id");
        return key.longValue();
    }

    public void insertOption(long questionId, AdminQuestionCreateFormDto.OptionForm opt, int displayOrder) {
        String sql = """
            INSERT INTO question_options
              (question_id, option_text, score, is_correct, display_order)
            VALUES
              (?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(
                sql,
                questionId,
                opt.getOptionText(),
                opt.getScore(),
                opt.isCorrect(),
                displayOrder
        );
    }
}
