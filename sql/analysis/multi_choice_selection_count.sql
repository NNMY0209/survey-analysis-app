SELECT
  q.question_id,
  q.question_text,
  qo.option_text,
  COUNT(*) AS selected_count
FROM question_answers qa
JOIN questions q ON q.question_id = qa.question_id
JOIN question_options qo ON qo.option_id = qa.option_id
WHERE q.survey_id = 1
  AND q.question_type = 'MULTI'
GROUP BY q.question_id, q.question_text, qo.option_text
ORDER BY selected_count DESC, qo.option_text;
