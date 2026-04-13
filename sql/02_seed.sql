CREATE DATABASE IF NOT EXISTS survey_app
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE survey_app;

-- ========================================
-- 02_seed.sql
-- 初期データ投入
-- ※ 01_schema.sql 実行後に流してください
-- ========================================


-- ========================================
-- 1. 管理者
-- login_id: admin
-- password: admin
-- ========================================
INSERT INTO admins (
  user_id,
  login_id,
  email,
  password_hash,
  user_name,
  role,
  is_active
) VALUES (
  1,
  'admin',
  'admin@example.com',
  '$2a$10$gPHtuganRf5Zx0StkJggju/QwDS1.izk12RHTylPYSEv4vFG9zmVy',
  '管理者 太郎',
  'ADMIN',
  1
);

-- ========================================
-- 2. アンケート
-- ========================================
INSERT INTO surveys (
  survey_id,
  title,
  description,
  consent_text,
  status,
  open_at,
  close_at,
  created_by,
  updated_by
) VALUES (
  1,
  'サンプル心理アンケート',
  'ポートフォリオ確認用のサンプルアンケートです。単一選択・複数選択・自由記述・数値入力に対応しています。',
  '本アンケートは学習用システムの動作確認を目的としたサンプルです。同意の上で回答してください。',
  'PUBLISHED',
  '2026-01-01 00:00:00',
  '2027-12-31 23:59:59',
  1,
  1
);

-- ========================================
-- 3. 設問
-- question_type:
--   SINGLE_CHOICE / MULTI_CHOICE / TEXT / NUMBER
-- ========================================
INSERT INTO questions (
  question_id,
  survey_id,
  question_text,
  question_type,
  question_role,
  is_reverse,
  is_required,
  display_order
) VALUES
(
  1,
  1,
  '最近1週間、気分は安定していましたか？',
  'SINGLE_CHOICE',
  'NORMAL',
  0,
  1,
  1
),
(
  2,
  1,
  '作業中に集中しやすかったですか？',
  'SINGLE_CHOICE',
  'NORMAL',
  0,
  1,
  2
),
(
  3,
  1,
  '最近感じているストレス要因を選んでください。（複数選択可）',
  'MULTI_CHOICE',
  'NORMAL',
  0,
  0,
  3
),
(
  4,
  1,
  '最近の体調について自由に記述してください。',
  'TEXT',
  'NORMAL',
  0,
  0,
  4
),
(
  5,
  1,
  '現在の疲労感を 1〜10 で入力してください。',
  'NUMBER',
  'NORMAL',
  0,
  1,
  5
);

-- ========================================
-- 4. 選択肢
-- ========================================
INSERT INTO question_options (
  option_id,
  question_id,
  option_text,
  score,
  is_correct,
  display_order
) VALUES
-- Q1
(1, 1, 'まったく当てはまらない', 1, 0, 1),
(2, 1, 'あまり当てはまらない', 2, 0, 2),
(3, 1, 'やや当てはまる', 3, 0, 3),
(4, 1, 'とても当てはまる', 4, 0, 4),

-- Q2
(5, 2, 'まったく当てはまらない', 1, 0, 1),
(6, 2, 'あまり当てはまらない', 2, 0, 2),
(7, 2, 'やや当てはまる', 3, 0, 3),
(8, 2, 'とても当てはまる', 4, 0, 4),

-- Q3
(9,  3, '仕事・学業', NULL, 0, 1),
(10, 3, '人間関係', NULL, 0, 2),
(11, 3, '睡眠不足', NULL, 0, 3),
(12, 3, '金銭面', NULL, 0, 4);

-- ========================================
-- 5. 尺度
-- ========================================
INSERT INTO scales (
  scale_id,
  survey_id,
  scale_code,
  scale_name,
  description
) VALUES
(
  1,
  1,
  'MENTAL',
  'メンタル安定尺度',
  '気分安定や集中状態を測るサンプル下位尺度'
),
(
  2,
  1,
  'STRESS',
  'ストレス要因尺度',
  'ストレス関連項目をまとめるサンプル下位尺度'
);

-- ========================================
-- 6. 尺度 × 設問
-- ========================================
INSERT INTO scale_questions (
  scale_question_id,
  scale_id,
  question_id,
  weight
) VALUES
(1, 1, 1, 1.00),
(2, 1, 2, 1.00),
(3, 2, 3, 1.00),
(4, 2, 5, 1.00);

-- ========================================
-- 7. 回答者
-- ========================================
INSERT INTO respondents (
  respondent_id,
  survey_id,
  respondent_key
) VALUES
(1, 1, 'sample-user-001'),
(2, 1, 'sample-user-002');

-- ========================================
-- 8. 回答セッション
-- status: COMPLETED / IN_PROGRESS
-- ========================================
INSERT INTO response_sessions (
  response_id,
  respondent_id,
  status,
  started_at,
  completed_at
) VALUES
(
  1,
  1,
  'COMPLETED',
  '2026-04-01 10:00:00',
  '2026-04-01 10:05:00'
),
(
  2,
  2,
  'COMPLETED',
  '2026-04-02 11:00:00',
  '2026-04-02 11:06:00'
);

-- ========================================
-- 9. 回答（単一選択・自由記述・数値）
-- MULTI_CHOICE は question_answers に親行を入れた上で
-- question_answer_multi に紐付けます
-- ========================================
INSERT INTO question_answers (
  answer_id,
  response_id,
  question_id,
  option_id,
  answer_number,
  answer_text
) VALUES
-- 回答者1
(1, 1, 1, 4, NULL, NULL),                  -- Q1: とても当てはまる
(2, 1, 2, 3, NULL, NULL),                  -- Q2: やや当てはまる
(3, 1, 3, NULL, NULL, NULL),               -- Q3: 複数選択の親行
(4, 1, 4, NULL, NULL, '寝不足気味ですが、全体としては落ち着いています。'),
(5, 1, 5, NULL, 4, NULL),

-- 回答者2
(6, 2, 1, 3, NULL, NULL),                  -- Q1: やや当てはまる
(7, 2, 2, 2, NULL, NULL),                  -- Q2: あまり当てはまらない
(8, 2, 3, NULL, NULL, NULL),               -- Q3: 複数選択の親行
(9, 2, 4, NULL, NULL, '仕事量が多く、少し疲れを感じています。'),
(10, 2, 5, NULL, 7, NULL);

-- ========================================
-- 10. 複数選択回答
-- ========================================
INSERT INTO question_answer_multi (
  answer_multi_id,
  answer_id,
  option_id
) VALUES
-- 回答者1 Q3
(1, 3, 9),   -- 仕事・学業
(2, 3, 11),  -- 睡眠不足

-- 回答者2 Q3
(3, 8, 10),  -- 人間関係
(4, 8, 12);  -- 金銭面