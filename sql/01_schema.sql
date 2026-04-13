-- DB作成（必要なら）
CREATE DATABASE IF NOT EXISTS survey_app
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE survey_app;

-- 既存がある場合は依存順にDROP（開発中だけ推奨）
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS response_sessions;
DROP TABLE IF EXISTS respondents;
DROP TABLE IF EXISTS question_options;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS surveys;
DROP TABLE IF EXISTS admins;

CREATE DATABASE IF NOT EXISTS survey_app
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE survey_app;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS question_answer_multi;
DROP TABLE IF EXISTS scale_questions;
DROP TABLE IF EXISTS question_answers;
DROP TABLE IF EXISTS response_sessions;
DROP TABLE IF EXISTS respondents;
DROP TABLE IF EXISTS question_options;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS scales;
DROP TABLE IF EXISTS surveys;
DROP TABLE IF EXISTS admins;

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 1. admins
-- ========================================
CREATE TABLE admins (
  user_id        BIGINT NOT NULL AUTO_INCREMENT,
  login_id       VARCHAR(50) NOT NULL,
  email          VARCHAR(255) NULL,
  password_hash  VARCHAR(255) NOT NULL,
  user_name      VARCHAR(100) NOT NULL,
  role           VARCHAR(20) NOT NULL,
  is_active      TINYINT(1) NOT NULL DEFAULT 1,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_admins_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 2. surveys
-- ========================================
CREATE TABLE surveys (
  survey_id      BIGINT NOT NULL AUTO_INCREMENT,
  title          VARCHAR(200) NOT NULL,
  description    TEXT NULL,
  consent_text   TEXT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  open_at        DATETIME NULL,
  close_at       DATETIME NULL,
  created_by     BIGINT NOT NULL,
  updated_by     BIGINT NOT NULL,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (survey_id),
  KEY idx_surveys_status (status),
  KEY idx_surveys_open_at (open_at),
  KEY idx_surveys_close_at (close_at),
  KEY idx_surveys_created_by (created_by),
  KEY idx_surveys_updated_by (updated_by),
  CONSTRAINT fk_surveys_created_by
    FOREIGN KEY (created_by) REFERENCES admins(user_id),
  CONSTRAINT fk_surveys_updated_by
    FOREIGN KEY (updated_by) REFERENCES admins(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 3. questions
-- ========================================
CREATE TABLE questions (
  question_id     BIGINT NOT NULL AUTO_INCREMENT,
  survey_id       BIGINT NOT NULL,
  question_text   TEXT NOT NULL,
  question_type   VARCHAR(30) NOT NULL,
  question_role   VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
  is_reverse      TINYINT(1) NOT NULL DEFAULT 0,
  is_required     TINYINT(1) NOT NULL DEFAULT 1,
  display_order   INT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (question_id),
  KEY idx_questions_survey (survey_id),
  KEY idx_questions_survey_order (survey_id, display_order),
  UNIQUE KEY uq_questions_survey_display_order (survey_id, display_order),
  CONSTRAINT fk_questions_survey
    FOREIGN KEY (survey_id) REFERENCES surveys(survey_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 4. question_options
-- ========================================
CREATE TABLE question_options (
  option_id       BIGINT NOT NULL AUTO_INCREMENT,
  question_id     BIGINT NOT NULL,
  option_text     VARCHAR(200) NOT NULL,
  score           INT NULL,
  is_correct      TINYINT(1) NOT NULL DEFAULT 0,
  display_order   INT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (option_id),
  KEY idx_options_question (question_id),
  KEY idx_options_question_order (question_id, display_order),
  CONSTRAINT fk_options_question
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 5. scales
-- ========================================
CREATE TABLE scales (
  scale_id        BIGINT NOT NULL AUTO_INCREMENT,
  survey_id       BIGINT NOT NULL,
  scale_code      VARCHAR(50) NOT NULL,
  scale_name      VARCHAR(100) NOT NULL,
  description     TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (scale_id),
  UNIQUE KEY uq_scales_survey_code (survey_id, scale_code),
  KEY idx_scales_survey (survey_id),
  CONSTRAINT fk_scales_survey
    FOREIGN KEY (survey_id) REFERENCES surveys(survey_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 6. scale_questions
-- ========================================
CREATE TABLE scale_questions (
  scale_question_id BIGINT NOT NULL AUTO_INCREMENT,
  scale_id          BIGINT NOT NULL,
  question_id       BIGINT NOT NULL,
  weight            DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (scale_question_id),
  UNIQUE KEY uq_scale_questions (scale_id, question_id),
  KEY idx_scale_questions_question (question_id),
  CONSTRAINT fk_scale_questions_scale
    FOREIGN KEY (scale_id) REFERENCES scales(scale_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_scale_questions_question
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 7. respondents
-- ========================================
CREATE TABLE respondents (
  respondent_id   BIGINT NOT NULL AUTO_INCREMENT,
  survey_id       BIGINT NOT NULL,
  respondent_key  VARCHAR(64) NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (respondent_id),
  UNIQUE KEY uq_respondents_survey_key (survey_id, respondent_key),
  KEY idx_respondents_survey (survey_id),
  CONSTRAINT fk_respondents_survey
    FOREIGN KEY (survey_id) REFERENCES surveys(survey_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 8. response_sessions
-- ========================================
CREATE TABLE response_sessions (
  response_id     BIGINT NOT NULL AUTO_INCREMENT,
  respondent_id   BIGINT NOT NULL,
  status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at    DATETIME NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (response_id),
  KEY idx_responses_respondent (respondent_id),
  KEY idx_responses_status (status),
  CONSTRAINT fk_responses_respondent
    FOREIGN KEY (respondent_id) REFERENCES respondents(respondent_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 9. question_answers
-- ========================================
CREATE TABLE question_answers (
  answer_id       BIGINT NOT NULL AUTO_INCREMENT,
  response_id     BIGINT NOT NULL,
  question_id     BIGINT NOT NULL,
  option_id       BIGINT NULL,
  answer_number   INT NULL,
  answer_text     TEXT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (answer_id),
  UNIQUE KEY uq_answers_response_question (response_id, question_id),
  KEY idx_answers_response (response_id),
  KEY idx_answers_question (question_id),
  KEY idx_answers_option (option_id),
  CONSTRAINT fk_answers_response
    FOREIGN KEY (response_id) REFERENCES response_sessions(response_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_answers_question
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_answers_option
    FOREIGN KEY (option_id) REFERENCES question_options(option_id)
    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ========================================
-- 10. question_answer_multi
-- ========================================
CREATE TABLE question_answer_multi (
  answer_multi_id BIGINT NOT NULL AUTO_INCREMENT,
  answer_id       BIGINT NOT NULL,
  option_id       BIGINT NOT NULL,
  created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (answer_multi_id),
  UNIQUE KEY uq_question_answer_multi (answer_id, option_id),
  KEY idx_qam_option (option_id),
  CONSTRAINT fk_qam_answer
    FOREIGN KEY (answer_id) REFERENCES question_answers(answer_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_qam_option
    FOREIGN KEY (option_id) REFERENCES question_options(option_id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;