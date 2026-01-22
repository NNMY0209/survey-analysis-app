package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.app.dto.ScaleDto;

@Repository
public class ScaleAdminDao {
	private final JdbcTemplate jdbcTemplate;

	public ScaleAdminDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<ScaleDto> findBySurveyId(long surveyId) {
		String sql = """
				SELECT scale_id, scale_code, scale_name, description
				FROM scales
				WHERE survey_id = ?
				ORDER BY scale_code
				""";
		return jdbcTemplate.query(sql, (rs, n) -> {
			ScaleDto d = new ScaleDto();
			d.setScaleId(rs.getLong("scale_id"));
			d.setScaleCode(rs.getString("scale_code"));
			d.setScaleName(rs.getString("scale_name"));
			d.setDescription(rs.getString("description"));
			return d;
		}, surveyId);
	}

	public void insert(long surveyId, String code, String name, String description) {
		String sql = """
				INSERT INTO scales (survey_id, scale_code, scale_name, description)
				VALUES (?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql, surveyId, code, name, description);
	}
}
