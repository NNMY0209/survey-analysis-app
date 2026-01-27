package com.example.app.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dao.ScaleImportDao;
import com.example.app.dto.ImportResultDto;

@Service
public class ScaleImportService {

	private final ScaleImportDao scaleImportDao;
	private final AdminResponseDao adminResponseDao;

	public ScaleImportService(ScaleImportDao scaleImportDao, AdminResponseDao adminResponseDao) {
		this.scaleImportDao = scaleImportDao;
		this.adminResponseDao = adminResponseDao;
	}

	/**
	 * 互換用（旧画面向け）：
	 * - scales.csv（尺度定義）を upsert
	 * - scale_questions.csv（尺度×設問×重み）を全置換
	 *
	 * ※ 新画面（1ページ手動ステップ）では、
	 *    importScalesOnly / importScaleQuestionsOnly を個別に呼ぶ想定。
	 */
	@Transactional
	public ImportResultDto importCsv(long surveyId, MultipartFile scalesFile, MultipartFile scaleQuestionsFile) {
		// 旧仕様は「両方必須」だったので、ここでは分割メソッドを使って同等動作にする
		ImportResultDto r1 = importScalesOnly(surveyId, scalesFile);
		if (!r1.getErrors().isEmpty()) {
			return r1;
		}

		ImportResultDto r2 = importScaleQuestionsOnly(surveyId, scaleQuestionsFile);

		// 合算（見やすさ用）
		r2.setTotalRows(r1.getTotalRows() + r2.getTotalRows());
		r2.setSuccessRows(r1.getSuccessRows() + r2.getSuccessRows());
		// errors は r2 に集約（r1は成功前提なので基本空だが念のため）
		r2.getErrors().addAll(r1.getErrors());

		return r2;
	}

	/**
	 * 新方式（1ページ手動ステップ向け）：
	 * scales.csv（尺度定義）のみ upsert
	 */
	@Transactional
	public ImportResultDto importScalesOnly(long surveyId, MultipartFile scalesFile) {
		ImportResultDto result = new ImportResultDto();

		// 倫理ガード：回答があるなら変更不可
		if (adminResponseDao.existsAnyResponseBySurveyId(surveyId)) {
			result.addError("回答が存在するため、下位尺度定義のインポートはできません。");
			return result;
		}

		if (scalesFile == null || scalesFile.isEmpty()) {
			result.addError("scales.csv を選択してください。");
			return result;
		}

		// 1) scales.csv 読み込み
		List<ScaleRow> scales = readScalesCsv(scalesFile, result);
		if (!result.getErrors().isEmpty())
			return result;

		// 2) upsert
		int success = 0;
		for (ScaleRow r : scales) {
			try {
				scaleImportDao.upsertScale(surveyId, r.scaleCode, r.scaleName, r.description);
				success++;
			} catch (Exception e) {
				result.addError("scales.csv：登録失敗 (" + r.scaleCode + "): " + e.getMessage());
			}
		}

		result.setTotalRows(scales.size());
		result.setSuccessRows(success);
		return result;
	}

	/**
	 * 新方式（1ページ手動ステップ向け）：
	 * scale_questions.csv（尺度×設問×重み）のみ 全置換
	 *
	 * 前提：
	 * - scales が存在する（scales.csvを先にimport）
	 * - questions が存在する（設問インポートを先に実施）
	 */
	@Transactional
	public ImportResultDto importScaleQuestionsOnly(long surveyId, MultipartFile scaleQuestionsFile) {
		ImportResultDto result = new ImportResultDto();

		// 倫理ガード：回答があるなら変更不可
		if (adminResponseDao.existsAnyResponseBySurveyId(surveyId)) {
			result.addError("回答が存在するため、下位尺度定義のインポートはできません。");
			return result;
		}

		if (scaleQuestionsFile == null || scaleQuestionsFile.isEmpty()) {
			result.addError("scale_questions.csv を選択してください。");
			return result;
		}

		// 参照用Map取得
		Map<String, Long> scaleIdByCode = scaleImportDao.findScaleIdByCode(surveyId);
		Map<Integer, Long> questionIdByOrder = scaleImportDao.findQuestionIdByOrder(surveyId);

		if (scaleIdByCode.isEmpty()) {
			result.addError("scales が0件です。先に scales.csv をインポートしてください。");
			return result;
		}
		if (questionIdByOrder.isEmpty()) {
			result.addError("このアンケートに設問がありません。先に設問定義をインポートしてください。");
			return result;
		}

		// scale_questions.csv 読み込み（整合性チェック込み）
		List<ScaleQuestionRow> sqRows = readScaleQuestionsCsv(scaleQuestionsFile, scaleIdByCode, questionIdByOrder,
				result);
		if (!result.getErrors().isEmpty())
			return result;

		// 全置換（survey単位）
		scaleImportDao.deleteScaleQuestionsBySurveyId(surveyId);

		// insert
		int success = 0;
		for (ScaleQuestionRow r : sqRows) {
			try {
				scaleImportDao.insert(r.scaleId, r.questionId, r.weight);
				success++;
			} catch (Exception e) {
				result.addError("scale_questions.csv：登録失敗 (scale=" + r.scaleCode + ", Q" + r.questionOrder + "): "
						+ e.getMessage());
			}
		}

		result.setTotalRows(sqRows.size());
		result.setSuccessRows(success);
		return result;
	}

	// ===== CSV row DTO（Service内クラスでOK） =====
	private static class ScaleRow {
		String scaleCode;
		String scaleName;
		String description;
	}

	private static class ScaleQuestionRow {
		String scaleCode;
		int questionOrder;
		BigDecimal weight;
		long scaleId;
		long questionId;
	}

	// ===== CSV reader =====

	private List<ScaleRow> readScalesCsv(MultipartFile file, ImportResultDto result) {
		List<ScaleRow> rows = new ArrayList<>();

		try (InputStream is = file.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String headerLine = br.readLine();
			if (headerLine == null || headerLine.isBlank()) {
				result.addError("scales.csv のヘッダが空です");
				return rows;
			}

			List<String> headers = parseCsvLine(headerLine);
			normalizeHeaders(headers);

			int codeIdx = indexOfIgnoreCase(headers, "scale_code");
			int nameIdx = indexOfIgnoreCase(headers, "scale_name");
			int descIdx = indexOfIgnoreCase(headers, "description"); // 任意

			if (codeIdx < 0 || nameIdx < 0) {
				result.addError("scales.csv に必須列 scale_code / scale_name が見つかりません");
				return rows;
			}

			int rowNo = 1;
			String line;
			while ((line = br.readLine()) != null) {
				rowNo++;
				if (line.isBlank())
					continue;

				List<String> cols = parseCsvLine(line);

				String code = trimBOM(getCol(cols, codeIdx));
				String name = trimBOM(getCol(cols, nameIdx));
				String desc = (descIdx >= 0) ? trimBOM(getCol(cols, descIdx)) : null;

				if (code == null)
					code = "";
				if (name == null)
					name = "";

				code = code.trim();
				name = name.trim();

				if (code.isBlank() || name.isBlank()) {
					result.addError("scales.csv 行" + rowNo + "：scale_code / scale_name は必須です");
					continue;
				}

				ScaleRow r = new ScaleRow();
				r.scaleCode = code;
				r.scaleName = name;
				r.description = (desc == null || desc.isBlank()) ? null : desc;
				rows.add(r);
			}

		} catch (Exception e) {
			result.addError("scales.csv 読み込みエラー: " + e.getMessage());
		}

		return rows;
	}

	private List<ScaleQuestionRow> readScaleQuestionsCsv(
			MultipartFile file,
			Map<String, Long> scaleIdByCode,
			Map<Integer, Long> questionIdByOrder,
			ImportResultDto result) {

		List<ScaleQuestionRow> rows = new ArrayList<>();

		try (InputStream is = file.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String headerLine = br.readLine();
			if (headerLine == null || headerLine.isBlank()) {
				result.addError("scale_questions.csv のヘッダが空です");
				return rows;
			}

			List<String> headers = parseCsvLine(headerLine);
			normalizeHeaders(headers);

			int codeIdx = indexOfIgnoreCase(headers, "scale_code");
			int orderIdx = indexOfIgnoreCase(headers, "question_order");
			if (orderIdx < 0)
				orderIdx = indexOfIgnoreCase(headers, "question_display_order"); // 別名許容
			int weightIdx = indexOfIgnoreCase(headers, "weight"); // 任意

			if (codeIdx < 0 || orderIdx < 0) {
				result.addError("scale_questions.csv に必須列 scale_code / question_order が見つかりません");
				return rows;
			}

			int rowNo = 1;
			String line;
			while ((line = br.readLine()) != null) {
				rowNo++;
				if (line.isBlank())
					continue;

				List<String> cols = parseCsvLine(line);

				String code = trimBOM(getCol(cols, codeIdx));
				String orderStr = trimBOM(getCol(cols, orderIdx));
				String weightStr = (weightIdx >= 0) ? trimBOM(getCol(cols, weightIdx)) : null;

				if (code == null)
					code = "";
				if (orderStr == null)
					orderStr = "";

				code = code.trim();
				orderStr = orderStr.trim();

				if (code.isBlank() || orderStr.isBlank()) {
					result.addError("scale_questions.csv 行" + rowNo + "：scale_code / question_order は必須です");
					continue;
				}

				Long scaleId = scaleIdByCode.get(code);
				if (scaleId == null) {
					result.addError("scale_questions.csv 行" + rowNo + "：存在しない scale_code: " + code
							+ "（先に scales.csv をインポートしてください）");
					continue;
				}

				Integer order;
				try {
					order = Integer.parseInt(orderStr);
				} catch (NumberFormatException e) {
					result.addError("scale_questions.csv 行" + rowNo + "：question_order が数値ではありません: " + orderStr);
					continue;
				}

				Long questionId = questionIdByOrder.get(order);
				if (questionId == null) {
					result.addError("scale_questions.csv 行" + rowNo + "：存在しない設問順です: Q" + order);
					continue;
				}

				BigDecimal weight = BigDecimal.ONE;
				if (weightStr != null && !weightStr.trim().isBlank()) {
					try {
						weight = new BigDecimal(weightStr.trim());
					} catch (Exception e) {
						result.addError("scale_questions.csv 行" + rowNo + "：weight が不正です: " + weightStr);
						continue;
					}
				}

				ScaleQuestionRow r = new ScaleQuestionRow();
				r.scaleCode = code;
				r.questionOrder = order;
				r.weight = weight;
				r.scaleId = scaleId;
				r.questionId = questionId;

				rows.add(r);
			}

		} catch (Exception e) {
			result.addError("scale_questions.csv 読み込みエラー: " + e.getMessage());
		}

		return rows;
	}

	// ===== util =====

	private static void normalizeHeaders(List<String> headers) {
		for (int i = 0; i < headers.size(); i++) {
			headers.set(i, trimBOM(headers.get(i)).trim());
		}
	}

	private static String trimBOM(String s) {
		if (s == null)
			return null;
		return s.replace("\uFEFF", "");
	}

	private static int indexOfIgnoreCase(List<String> headers, String key) {
		for (int i = 0; i < headers.size(); i++) {
			String h = headers.get(i);
			if (h != null && h.equalsIgnoreCase(key))
				return i;
		}
		return -1;
	}

	private static String getCol(List<String> cols, int idx) {
		if (idx < 0 || cols == null || idx >= cols.size())
			return null;
		return cols.get(idx);
	}

	/** 簡易CSV（"..." と "" エスケープまで対応） */
	private static List<String> parseCsvLine(String line) {
		List<String> out = new ArrayList<>();
		if (line == null)
			return out;

		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					sb.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				out.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		out.add(sb.toString());
		return out;
	}
}
