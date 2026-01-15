package com.example.app.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionAnswerDao {

	private final JdbcTemplate jdbcTemplate;

	public QuestionAnswerDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 回答を全削除（再送信に強い）
	 * - 先に中間テーブルを消してから question_answers を消す
	 *   ※FKのON DELETE CASCADEがあっても明示しておくと安全
	 */
	public void deleteByResponseId(long responseId) {
		// response_id -> answer_id をたどって multi を削除
		jdbcTemplate.update("""
				    DELETE qam
				      FROM question_answer_multi qam
				      JOIN question_answers qa ON qa.answer_id = qam.answer_id
				     WHERE qa.response_id = ?
				""", responseId);

		jdbcTemplate.update("DELETE FROM question_answers WHERE response_id = ?", responseId);
	}

	/** question_answers の親行を作成/取得（response_id + question_id 一意） */
	private long upsertAnswerRow(long responseId, long questionId) {
		jdbcTemplate.update("""
				    INSERT INTO question_answers
				      (response_id, question_id, option_id, answer_number, answer_text, created_at)
				    VALUES (?, ?, NULL, NULL, NULL, NOW())
				    ON DUPLICATE KEY UPDATE answer_id = LAST_INSERT_ID(answer_id)
				""", responseId, questionId);

		Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
		return id != null ? id : 0L;
	}

	/** SINGLE：親行に option_id を保存 */
	public void insertSingle(long responseId, long questionId, long optionId) {
		upsertAnswerRow(responseId, questionId);
		jdbcTemplate.update("""
				    UPDATE question_answers
				       SET option_id = ?,
				           answer_number = NULL,
				           answer_text = NULL
				     WHERE response_id = ? AND question_id = ?
				""", optionId, responseId, questionId);
	}

	/**
	 * MULTI：中間テーブルに保存
	 * - 親行を1つ作り
	 * - 既存の選択肢は削除して入れ直す（再送信時に安全）
	 */
	public void insertMulti(long responseId, long questionId, List<Long> optionIds) {
		long answerId = upsertAnswerRow(responseId, questionId);

		// 念のため親行側はクリア（事故防止）
		jdbcTemplate.update("""
				    UPDATE question_answers
				       SET option_id = NULL,
				           answer_number = NULL,
				           answer_text = NULL
				     WHERE answer_id = ?
				""", answerId);

		// 既存のMULTIを削除して入れ直し
		jdbcTemplate.update("DELETE FROM question_answer_multi WHERE answer_id = ?", answerId);

		String sql = "INSERT INTO question_answer_multi (answer_id, option_id, created_at) VALUES (?, ?, NOW())";
		for (Long optionId : optionIds) {
			jdbcTemplate.update(sql, answerId, optionId);
		}
	}

	/** TEXT/NUMBER：answer_text に保存（最短運用） */
	public void insertText(long responseId, long questionId, String answerText) {
		upsertAnswerRow(responseId, questionId);
		jdbcTemplate.update("""
				    UPDATE question_answers
				       SET option_id = NULL,
				           answer_number = 1,
				           answer_text = ?
				     WHERE response_id = ? AND question_id = ?
				""", answerText, responseId, questionId);
	}
}
