package com.example.app.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dao.QuestionImportDao;
import com.example.app.dao.ScaleImportDao;
import com.example.app.dto.ImportResultDto;

@Service
public class QuestionImportService {

	private final QuestionImportDao questionImportDao;
	private final ScaleImportDao scaleImportDao;
	private final AdminResponseDao adminResponseDao;

	public QuestionImportService(
			QuestionImportDao questionImportDao,
			ScaleImportDao scaleImportDao,
			AdminResponseDao adminResponseDao) {
		this.questionImportDao = questionImportDao;
		this.scaleImportDao = scaleImportDao;
		this.adminResponseDao = adminResponseDao;
	}

	public ImportResultDto importCsv(long surveyId, MultipartFile questionsFile, MultipartFile optionsFile) {
		ImportResultDto result = new ImportResultDto();

		// 倫理ガード：回答があるなら設問定義変更不可
		if (adminResponseDao.existsAnyResponseBySurveyId(surveyId)) {
			result.addError("回答が存在するため、設問定義のインポートはできません。");
			return result;
		}

		try {
			Map<Integer, QRow> questions = readQuestionsCsv(surveyId, questionsFile, result);
			Map<Integer, List<ORow>> options = readOptionsCsv(surveyId, optionsFile, result);

			if (!result.getErrors().isEmpty())
				return result;

			// 整合性：選択肢必須なタイプは2件以上
			for (var e : questions.entrySet()) {
				int qOrder = e.getKey();
				QRow q = e.getValue();

				if (needsOptions(q.questionType)) {
					List<ORow> ops = options.getOrDefault(qOrder, List.of());
					if (ops.size() < 2) {
						result.addError("設問 display_order=" + qOrder + " は選択肢が2件以上必要です。");
					}
				}
			}

			// 整合性：scales 指定がある場合、scale_code が存在するか確認
			// （DBを叩くので、先にまとめてチェック）
			for (var e : questions.entrySet()) {
				int qOrder = e.getKey();
				QRow q = e.getValue();

				for (var entry : q.scales.entrySet()) {
					String code = entry.getKey();
					if (scaleImportDao.findScaleId(surveyId, code).isEmpty()) {
						result.addError("設問 display_order=" + qOrder + " の scales に存在しない scale_code があります: " + code);
					}
				}
			}

			if (!result.getErrors().isEmpty())
				return result;

			// 反映（トランザクション）
			applyImportTransactional(surveyId, questions, options, result);
			return result;

		} catch (Exception e) {
			result.addError("CSV読み込みエラー: " + e.getMessage());
			return result;
		}
	}

	@Transactional
	protected void applyImportTransactional(
			long surveyId,
			Map<Integer, QRow> questions,
			Map<Integer, List<ORow>> options,
			ImportResultDto result) {
		int total = 0;
		int success = 0;

		List<Integer> orders = new ArrayList<>(questions.keySet());
		Collections.sort(orders);

		for (int qOrder : orders) {
			total++;
			QRow q = questions.get(qOrder);

			// upsert question
			long questionId = questionImportDao.findQuestionId(surveyId, q.displayOrder)
					.map(id -> {
						questionImportDao.updateQuestion(id, q.questionText, q.questionType, q.questionRole,
								q.isReverse, q.isRequired);
						return id;
					})
					.orElseGet(() -> questionImportDao.insertQuestion(
							surveyId, q.displayOrder, q.questionText, q.questionType, q.questionRole, q.isReverse,
							q.isRequired));

			// options：全置換
			questionImportDao.deleteOptionsByQuestionId(questionId);
			if (needsOptions(q.questionType)) {
				List<ORow> ops = new ArrayList<>(options.getOrDefault(qOrder, List.of()));
				ops.sort(Comparator.comparingInt(o -> o.displayOrder));
				for (ORow o : ops) {
					questionImportDao.insertOption(questionId, o.displayOrder, o.optionText, o.score, o.isCorrect);
				}
			}

			// scales：全置換
			scaleImportDao.deleteByQuestionId(questionId);
			if (!q.scales.isEmpty()) {
				for (var entry : q.scales.entrySet()) {
					String scaleCode = entry.getKey();
					BigDecimal weight = entry.getValue();

					Long scaleId = scaleImportDao.findScaleId(surveyId, scaleCode)
							.orElseThrow(() -> new IllegalStateException("scale_code not found: " + scaleCode));

					scaleImportDao.insert(scaleId, questionId, weight);
				}
			}

			success++;
		}

		result.setTotalRows(total);
		result.setSuccessRows(success);
	}

	// ================= CSV読み取り =================

	/**
	 * questions.csv
	 * 必須: survey_id, display_order, question_text, question_type
	 * 任意: question_role, is_reverse, is_required, scales
	 *
	 * scales列: "CODE:1.0|CODE2:0.5"（weight省略時は1.0）
	 */
	private Map<Integer, QRow> readQuestionsCsv(long surveyId, MultipartFile file, ImportResultDto result)
			throws IOException {
		Map<Integer, QRow> map = new LinkedHashMap<>();

		try (InputStream is = file.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String header = br.readLine();
			if (header == null || header.isBlank()) {
				result.addError("questions.csv のヘッダが空です");
				return map;
			}

			List<String> headers = parseCsvLine(header);
			normalizeHeaders(headers);

			int idxSurveyId = indexOfIgnoreCase(headers, "survey_id");
			int idxOrder = indexOfIgnoreCase(headers, "display_order");
			int idxText = indexOfIgnoreCase(headers, "question_text");
			int idxType = indexOfIgnoreCase(headers, "question_type");

			int idxRole = indexOfIgnoreCase(headers, "question_role");
			int idxRev = indexOfIgnoreCase(headers, "is_reverse");
			int idxReq = indexOfIgnoreCase(headers, "is_required");
			int idxScales = indexOfIgnoreCase(headers, "scales"); // 追加

			if (idxSurveyId < 0 || idxOrder < 0 || idxText < 0 || idxType < 0) {
				result.addError("questions.csv 必須列: survey_id, display_order, question_text, question_type");
				return map;
			}

			int rowNo = 1;
			String line;
			while ((line = br.readLine()) != null) {
				rowNo++;
				if (line.isBlank())
					continue;

				List<String> cols = parseCsvLine(line);

				long sid = parseLongStrict(getCol(cols, idxSurveyId), "questions.csv 行" + rowNo + " survey_id", result);
				if (sid != surveyId) {
					result.addError("questions.csv 行" + rowNo + " の survey_id が不一致: " + sid);
					continue;
				}

				int order = parseIntStrict(getCol(cols, idxOrder), "questions.csv 行" + rowNo + " display_order",
						result);
				String text = safe(getCol(cols, idxText));
				String type = safe(getCol(cols, idxType)).toUpperCase(Locale.ROOT);

				if (text.isBlank()) {
					result.addError("questions.csv 行" + rowNo + " question_text が空です");
					continue;
				}

				if (!Set.of("SINGLE_CHOICE", "MULTI_CHOICE", "TEXT", "NUMBER").contains(type)) {
					result.addError("questions.csv 行" + rowNo + " question_type が不正: " + type);
					continue;
				}

				String role = (idxRole >= 0) ? safe(getCol(cols, idxRole)).toUpperCase(Locale.ROOT) : "NORMAL";
				if (role.isBlank())
					role = "NORMAL";

				// ここで role を制限したいなら（任意）
				// if (!Set.of("NORMAL","ATTENTION_CHECK","VALIDITY_CHECK").contains(role)) { ... }

				boolean rev = (idxRev >= 0) ? parseBool01(getCol(cols, idxRev)) : false;
				boolean req = (idxReq >= 0) ? parseBool01(getCol(cols, idxReq)) : true;

				String scalesRaw = (idxScales >= 0) ? safe(getCol(cols, idxScales)) : "";
				Map<String, BigDecimal> scales = parseScales(scalesRaw, "questions.csv 行" + rowNo + " scales", result);

				if (map.containsKey(order)) {
					result.addError("questions.csv display_order が重複しています: " + order);
					continue;
				}

				map.put(order, new QRow(order, text, type, role, rev, req, scales));
			}
		}

		return map;
	}

	/**
	 * question_options.csv
	 * 必須: survey_id, question_display_order, display_order, option_text, score
	 * 任意: is_correct
	 */
	private Map<Integer, List<ORow>> readOptionsCsv(long surveyId, MultipartFile file, ImportResultDto result)
			throws IOException {
		Map<Integer, List<ORow>> map = new HashMap<>();

		try (InputStream is = file.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String header = br.readLine();
			if (header == null || header.isBlank()) {
				result.addError("question_options.csv のヘッダが空です");
				return map;
			}

			List<String> headers = parseCsvLine(header);
			normalizeHeaders(headers);

			int idxSurveyId = indexOfIgnoreCase(headers, "survey_id");
			int idxQOrder = indexOfIgnoreCase(headers, "question_display_order");
			int idxOrder = indexOfIgnoreCase(headers, "display_order");
			int idxText = indexOfIgnoreCase(headers, "option_text");
			int idxScore = indexOfIgnoreCase(headers, "score");
			int idxCorrect = indexOfIgnoreCase(headers, "is_correct");

			if (idxSurveyId < 0 || idxQOrder < 0 || idxOrder < 0 || idxText < 0 || idxScore < 0) {
				result.addError(
						"question_options.csv 必須列: survey_id, question_display_order, display_order, option_text, score");
				return map;
			}

			int rowNo = 1;
			String line;
			while ((line = br.readLine()) != null) {
				rowNo++;
				if (line.isBlank())
					continue;

				List<String> cols = parseCsvLine(line);

				long sid = parseLongStrict(getCol(cols, idxSurveyId), "options 行" + rowNo + " survey_id", result);
				if (sid != surveyId) {
					result.addError("question_options.csv 行" + rowNo + " の survey_id が不一致: " + sid);
					continue;
				}

				int qOrder = parseIntStrict(getCol(cols, idxQOrder), "options 行" + rowNo + " question_display_order",
						result);
				int oOrder = parseIntStrict(getCol(cols, idxOrder), "options 行" + rowNo + " display_order", result);

				String text = safe(getCol(cols, idxText));
				if (text.isBlank()) {
					result.addError("question_options.csv 行" + rowNo + " option_text が空です");
					continue;
				}

				int score = parseIntStrict(getCol(cols, idxScore), "options 行" + rowNo + " score", result);
				boolean correct = (idxCorrect >= 0) ? parseBool01(getCol(cols, idxCorrect)) : false;

				map.computeIfAbsent(qOrder, k -> new ArrayList<>())
						.add(new ORow(qOrder, oOrder, text, score, correct));
			}
		}

		return map;
	}

	// ================= 仕様ロジック =================

	private boolean needsOptions(String questionType) {
		return "SINGLE_CHOICE".equalsIgnoreCase(questionType) || "MULTI_CHOICE".equalsIgnoreCase(questionType);
	}

	/**
	 * scales列:
	 *  - "CODE:1.0|CODE2:0.5"
	 *  - weight省略時は1.0
	 */
	private Map<String, BigDecimal> parseScales(String raw, String label, ImportResultDto result) {
		Map<String, BigDecimal> map = new LinkedHashMap<>();
		if (raw == null || raw.isBlank())
			return map;

		String[] parts = raw.split("\\|");
		for (String p : parts) {
			String part = p.trim();
			if (part.isEmpty())
				continue;

			String[] pair = part.split(":", 2);
			String code = pair[0].trim();
			if (code.isEmpty()) {
				result.addError(label + " に空のscale_codeがあります: " + raw);
				continue;
			}

			BigDecimal weight = BigDecimal.ONE;
			if (pair.length == 2) {
				String w = pair[1].trim();
				if (!w.isEmpty()) {
					try {
						weight = new BigDecimal(w);
					} catch (Exception e) {
						result.addError(label + " の weight が数値ではありません: " + w + " (raw=" + raw + ")");
						continue;
					}
				}
			}

			if (map.containsKey(code)) {
				result.addError(label + " にscale_codeの重複があります: " + code + " (raw=" + raw + ")");
				continue;
			}

			map.put(code, weight);
		}
		return map;
	}

	// ================= CSVユーティリティ（AnswerImportServiceに寄せるなら共通化OK） =================

	private List<String> parseCsvLine(String line) {
		List<String> out = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		boolean inQuote = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					sb.append('"');
					i++;
				} else {
					inQuote = !inQuote;
				}
			} else if (c == ',' && !inQuote) {
				out.add(sb.toString());
				sb.setLength(0);
			} else {
				sb.append(c);
			}
		}
		out.add(sb.toString());
		return out;
	}

	private void normalizeHeaders(List<String> headers) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i) != null) {
				headers.set(i, headers.get(i).replace("\uFEFF", "").trim());
			}
		}
	}

	private int indexOfIgnoreCase(List<String> headers, String name) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i) != null && headers.get(i).equalsIgnoreCase(name))
				return i;
		}
		return -1;
	}

	private String getCol(List<String> cols, int idx) {
		if (idx < 0)
			return null;
		if (idx >= cols.size())
			return null;
		String v = cols.get(idx);
		return (v == null) ? null : v.replace("\uFEFF", "").trim();
	}

	private String safe(String s) {
		return (s == null) ? "" : s.replace("\uFEFF", "").trim();
	}

	private int parseIntStrict(String s, String label, ImportResultDto result) {
		try {
			return Integer.parseInt(safe(s));
		} catch (Exception e) {
			result.addError(label + " が数値ではありません: " + s);
			return 0;
		}
	}

	private long parseLongStrict(String s, String label, ImportResultDto result) {
		try {
			return Long.parseLong(safe(s));
		} catch (Exception e) {
			result.addError(label + " が数値ではありません: " + s);
			return 0L;
		}
	}

	private boolean parseBool01(String s) {
		String v = safe(s);
		return "1".equals(v) || "true".equalsIgnoreCase(v);
	}

	// ================= 内部DTO =================

	private static class QRow {
		final int displayOrder;
		final String questionText;
		final String questionType;
		final String questionRole;
		final boolean isReverse;
		final boolean isRequired;
		final Map<String, BigDecimal> scales; // scale_code -> weight

		QRow(int displayOrder, String questionText, String questionType, String questionRole,
				boolean isReverse, boolean isRequired, Map<String, BigDecimal> scales) {
			this.displayOrder = displayOrder;
			this.questionText = questionText;
			this.questionType = questionType;
			this.questionRole = questionRole;
			this.isReverse = isReverse;
			this.isRequired = isRequired;
			this.scales = (scales == null) ? Map.of() : scales;
		}
	}

	private static class ORow {
		final int questionDisplayOrder;
		final int displayOrder;
		final String optionText;
		final int score;
		final boolean isCorrect;

		ORow(int questionDisplayOrder, int displayOrder, String optionText, int score, boolean isCorrect) {
			this.questionDisplayOrder = questionDisplayOrder;
			this.displayOrder = displayOrder;
			this.optionText = optionText;
			this.score = score;
			this.isCorrect = isCorrect;
		}
	}
}
