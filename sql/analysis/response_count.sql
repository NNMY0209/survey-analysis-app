SELECT survey_id, COUNT(*) AS response_count
FROM response_sessions
WHERE status = 'COMPLETED'
GROUP BY survey_id;
