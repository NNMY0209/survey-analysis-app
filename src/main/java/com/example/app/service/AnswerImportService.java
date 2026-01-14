package com.example.app.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.app.dao.AnswerImportDao;
import com.example.app.dao.ImportMetaDao;
import com.example.app.dto.ImportOptionMetaDto;
import com.example.app.dto.ImportQuestionMetaDto;
import com.example.app.dto.ImportResultDto;

@Service
public class AnswerImportService {

	private final ImportMetaDao importMetaDao;
	private final AnswerImportDao answerImportDao;

	// "Q1" → 1 みたいに取りたい
	private static final Pattern Q_COL = Pattern.compile("^Q(\\d+)$");

	public AnswerImportService(ImportMetaDao importMetaDao, AnswerImportDao answerImportDao) {
		this.importMetaDao = importMetaDao;
		this.answerImportDao = answerImportDao;
	}

	/**
	 * CSVアップロード（surveyIdは遷移元で確定）
	 * CSVヘッダ例: respondent_key,Q1,Q2,Q3
	 */
	@Transactional
	public ImportResultDto importCsv(long surveyId, MultipartFile file) {

		ImportResultDto result = new ImportResultDto();

		// 1) メタ取得（設問タイプ判定・display_order→option_id変換に使う）
		List<ImportQuestionMetaDto> questions = importMetaDao.findQuestions(surveyId);
		List<ImportOptionMetaDto> options = importMetaDao.findOptions(surveyId);

		Map<Long, ImportQuestionMetaDto> questionById = new HashMap<>();
		for (ImportQuestionMetaDto q : questions) {
			questionById.put(q.getQuestionId(), q);
		}

		// question_id -> (display_order -> option_id)
		Map<Long, Map<Integer, Long>> optionIdByQuestionAndOrder = new HashMap<>();
		for (ImportOptionMetaDto o : options) {
			optionIdByQuestionAndOrder
					.computeIfAbsent(o.getQuestionId(), k -> new HashMap<>())
					.put(o.getDisplayOrder(), o.getOptionId());
		}

		// 2) CSV読み込み
		try (InputStream is = file.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			String headerLine = br.readLine();
			if (headerLine == null || headerLine.isBlank()) {
				result.addError("CSVヘッダが空です");
				result.setTotalRows(0);
				return result;
			}

			List<String> headers = parseCsvLine(headerLine);
			if (headers.isEmpty()) {
				result.addError("CSVヘッダの解析に失敗しました");
				result.setTotalRows(0);
				return result;
			}

			int respondentKeyIdx = indexOfIgnoreCase(headers, "respondent_key");
			if (respondentKeyIdx < 0) {
				result.addError("必須列 respondent_key が見つかりません");
				result.setTotalRows(0);
				return result;
			}

			// Q列（Q1,Q2,...）を question_id に変換して保持
			// colIndex -> questionId
			Map<Integer, Long> qColIndexToQuestionId = new LinkedHashMap<>();
			for (int i = 0; i < headers.size(); i++) {
				String h = headers.get(i).trim();
				Long qid = parseQuestionIdFromHeader(h);
				if (qid != null) {
					qColIndexToQuestionId.put(i, qid);
				}
			}

			if (qColIndexToQuestionId.isEmpty()) {
				result.addError("Q列（Q1,Q2,...）が見つかりません");
				result.setTotalRows(0);
				return result;
			}

			// ヘッダにあるQ列が、surveyIdの設問として存在するかチェック
			for (Long qid : qColIndexToQuestionId.values()) {
				if (!questionById.containsKey(qid)) {
					result.addError("このアンケートに存在しない設問がCSVヘッダにあります: Q" + qid);
				}
			}
			if (!result.getErrors().isEmpty()) {
				// 致命的：ヘッダが違う
				result.setTotalRows(0);
				return result;
			}

			// 3) 行処理
			int rowNo = 1; // ヘッダが1行目
			String line;
			while ((line = br.readLine()) != null) {
				rowNo++;

				if (line.isBlank())
					continue;

				result.setTotalRows(result.getTotalRows() + 1);

				List<String> cols = parseCsvLine(line);

				// 列数不足は許容（足りない分は空扱い）
				String respondentKey = getCol(cols, respondentKeyIdx);
				if (respondentKey == null || respondentKey.isBlank()) {
					// respondent_key 空なら自動採番
					respondentKey = "imp-" + UUID.randomUUID();
				}

				try {
					// 3-1) respondents & response_sessions 作成
					long respondentId = answerImportDao.createRespondent(surveyId, respondentKey);
					long responseId = answerImportDao.createCompletedSession(surveyId, respondentId);

					// 3-2) 各設問の値を保存
					for (Map.Entry<Integer, Long> entry : qColIndexToQuestionId.entrySet()) {
						int colIdx = entry.getKey();
						long questionId = entry.getValue();

						ImportQuestionMetaDto qmeta = questionById.get(questionId);
						String qType = qmeta.getQuestionType();

						String raw = getCol(cols, colIdx);
						if (raw == null)
							raw = "";
						raw = raw.trim();

						if (raw.isBlank()) {
							// 未回答は保存しない（空欄ありOK）
							continue;
						}

						if ("SINGLE".equalsIgnoreCase(qType)) {
							int order = parseIntStrict(raw);
							Long optionId = getOptionId(optionIdByQuestionAndOrder, questionId, order);
							if (optionId == null) {
								throw new IllegalArgumentException("Q" + questionId + " の選択肢が不正: " + raw);
							}
							answerImportDao.insertSingle(responseId, questionId, optionId);

						} else if ("MULTI".equalsIgnoreCase(qType)) {
							List<Integer> orders = parseMultiOrders(raw); // "1,3"
							List<Long> optionIds = new ArrayList<>();
							for (int order : orders) {
								Long optionId = getOptionId(optionIdByQuestionAndOrder, questionId, order);
								if (optionId == null) {
									throw new IllegalArgumentException("Q" + questionId + " の選択肢が不正: " + order);
								}
								optionIds.add(optionId);
							}
							answerImportDao.insertMulti(responseId, questionId, optionIds);

						} else if ("TEXT".equalsIgnoreCase(qType) || "NUMBER".equalsIgnoreCase(qType)) {
							// NUMBERもTEXT同様に answer_text へ保存
							answerImportDao.insertText(responseId, questionId, raw);

						} else {
							// 未知のタイプ
							throw new IllegalArgumentException(
									"未対応のquestion_type: " + qType + " (Q" + questionId + ")");
						}
					}

					result.setSuccessRows(result.getSuccessRows() + 1);

				} catch (Exception e) {
					// 1行単位でエラーを溜める（ロールバックはトランザクションの都合で注意）
					// → 本格運用は「1行ごとにトランザクション」だけど、実習ではまず動作優先でOK
					result.addError("行" + rowNo + " の取り込み失敗: " + e.getMessage());
				}
			}

		} catch (Exception e) {
			result.addError("CSV読み込みエラー: " + e.getMessage());
		}

		return result;
	}

	// -----------------
	// helper
	// -----------------

	private static int indexOfIgnoreCase(List<String> headers, String target) {
		for (int i = 0; i < headers.size(); i++) {
			if (headers.get(i) != null && headers.get(i).trim().equalsIgnoreCase(target)) {
				return i;
			}
		}
		return -1;
	}

	private static String getCol(List<String> cols, int idx) {
		if (idx < 0)
			return null;
		if (idx >= cols.size())
			return null;
		return cols.get(idx);
	}

	private static Long parseQuestionIdFromHeader(String header) {
		Matcher m = Q_COL.matcher(header);
		if (!m.matches())
			return null;
		return Long.parseLong(m.group(1));
	}

	private static int parseIntStrict(String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			throw new IllegalArgumentException("数値として解釈できません: " + s);
		}
	}

	private static List<Integer> parseMultiOrders(String raw) {
		// "1,3" / "1, 2, 3" / "\"1,3\"" などを想定
		String r = raw.trim();
		if (r.startsWith("\"") && r.endsWith("\"") && r.length() >= 2) {
			r = r.substring(1, r.length() - 1);
		}
		String[] parts = r.split(",");
		List<Integer> list = new ArrayList<>();
		for (String p : parts) {
			String t = p.trim();
			if (t.isEmpty())
				continue;
			list.add(parseIntStrict(t));
		}
		if (list.isEmpty()) {
			throw new IllegalArgumentException("MULTIの値が空です: " + raw);
		}
		return list;
	}

	private static Long getOptionId(Map<Long, Map<Integer, Long>> map, long questionId, int displayOrder) {
		Map<Integer, Long> m = map.get(questionId);
		if (m == null)
			return null;
		return m.get(displayOrder);
	}

	/**
	 * 簡易CSVパーサ（カンマ区切り＋ダブルクォートを最低限対応）
	 * - "1,3" のようなセルを1つの値として扱える
	 */
	private static List<String> parseCsvLine(String line) {
		List<String> out = new ArrayList<>();
		if (line == null)
			return out;

		StringBuilder sb = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '"') {
				// "" -> " のエスケープ
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					sb.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}

			if (c == ',' && !inQuotes) {
				out.add(sb.toString());
				sb.setLength(0);
				continue;
			}

			sb.append(c);
		}
		out.add(sb.toString());
		return out;
	}
}
