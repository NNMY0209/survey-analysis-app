SELECT
  q.question_id,
  q.question_text,
  AVG(
    CASE
      WHEN q.is_reverse = 1 THEN (6 - qo.score)
      ELSE qo.score
    END
  ) AS avg_score_adjusted
FROM question_answers qa
JOIN questions q ON q.question_id = qa.question_id
JOIN question_options qo ON qo.option_id = qa.option_id
WHERE q.survey_id = 1
  AND q.question_type = 'SINGLE'
GROUP BY q.question_id, q.question_text
ORDER BY q.question_id;
