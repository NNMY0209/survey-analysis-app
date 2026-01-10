package com.example.app.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DbcheckController {

	private final JdbcTemplate jdbcTemplate;

	public DbcheckController(JdbcTemplate jbdctemplate) {
		this.jdbcTemplate = jbdctemplate;
	}

	@GetMapping("/dbcheck")
	public String dbcheck(Model model) {
		Integer tableCount = jdbcTemplate.queryForObject(
				"select count(*) from information_schema.tables where table_schema = 'survey_app'", Integer.class);
		model.addAttribute("tableCount", tableCount);
		return "dbcheck";
	}
}