package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.app.dto.AdminDto;

@Repository
public class AdminDao {
	private final JdbcTemplate jdbcTemplate;

    public AdminDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AdminDto> rowMapper = (rs, rowNum) -> {
        AdminDto dto = new AdminDto();
        dto.setUserId(rs.getLong("user_id"));
        dto.setLoginId(rs.getString("login_id"));
        dto.setEmail(rs.getString("email"));
        dto.setPasswordHash(rs.getString("password_hash"));
        dto.setUserName(rs.getString("user_name"));
        dto.setRole(rs.getString("role"));
        dto.setIsActive(rs.getBoolean("is_active"));
        dto.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        dto.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return dto;
    };

    public AdminDto findByLoginId(String loginId) {
        String sql = """
                SELECT user_id, login_id, email, password_hash, user_name, role, is_active, created_at, updated_at
                FROM admins
                WHERE login_id = ?
                """;

        List<AdminDto> list = jdbcTemplate.query(sql, rowMapper, loginId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Long findUserIdByLoginId(String loginId) {
        String sql = "SELECT user_id FROM admins WHERE login_id = ?";
        List<Long> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("user_id"), loginId);
        return list.isEmpty() ? null : list.get(0);
    }
    
    public void insertAdmin(String loginId, String userName, String passwordHash) {

        String sql = """
            INSERT INTO admins (
                login_id,
                password_hash,
                user_name,
                role,
                is_active
            ) VALUES (?, ?, ?, 'ADMIN', 1)
        """;

        jdbcTemplate.update(sql, loginId, passwordHash, userName);
    }

}
