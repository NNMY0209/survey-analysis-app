SELECT
  rs.response_id,
  r.respondent_key,
  q.display_order,
  q.question_id,
  q.question_type,
  q.question_text,
  qo.option_text,
  qo.score
FROM response_sessions rs
JOIN respondents r ON r.respondent_id = rs.respondent_id
JOIN question_answers qa ON qa.response_id = rs.response_id
JOIN questions q ON q.question_id = qa.question_id
LEFT JOIN question_options qo ON qo.option_id = qa.option_id
WHERE rs.survey_id = 1
ORDER BY rs.response_id, q.display_order, qo.display_order;
