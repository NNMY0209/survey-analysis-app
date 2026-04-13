INSERT INTO surveys
(title, description, consent_text, status, open_at, close_at, created_by, created_at, updated_by, updated_at)
VALUES
('社員満足度アンケート', '社員向け満足度調査', '確認して同意してください', 'OPEN',
 '2025-01-01 00:00:00', '2025-12-31 23:59:59', 1, NOW(), 1, NOW()),

('新人研修アンケート', '研修内容の評価', '同意文', 'DRAFT',
 NULL, NULL, 1, NOW(), 1, NOW()),

('イベント満足度調査', '社内イベントの感想', '同意文', 'CLOSED',
 '2024-05-01 00:00:00', '2024-05-31 23:59:59', 1, NOW(), 1, NOW());
