SELECT s.survey_id, s.title, q.question_id, q.display_order, q.question_type, q.is_reverse, q.question_text
FROM surveys s
JOIN questions q ON q.survey_id = s.survey_id
WHERE s.survey_id = 1
ORDER BY q.display_order;
