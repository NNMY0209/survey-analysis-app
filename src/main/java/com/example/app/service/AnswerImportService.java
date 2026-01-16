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

	// "Q1" → 1 みたいに取りたい（CSVのQ番号 = questions.display_order）
	private static final Pattern Q_COL = Pattern.compile("^Q(\\d+)$");

	public AnswerImportService(ImportMetaDao importMetaDao, AnswerImportDao answerImportDao) {
		this.importMetaDao = importMetaDao;
		this.answerImportDao = answerImportDao;
	}

	/**
	 * CSVアップロード（surveyIdは遷移元で確定）
	 * CSVヘッダ例: respondent_key,Q1,Q2,Q3
	 *
	 * 仕様：
	 * - Q1/Q2... は question_id ではなく questions.display_order を意味する
	 * - 値（セル）は選択肢の display_order（例：5件法なら 1〜5）
	 */
	@Transactional
	public ImportResultDto importCsv(long surveyId, MultipartFile file) {

		ImportResultDto result = new ImportResultDto();

		// 1) メタ取得（設問タイプ判定・display_order→option_id変換に使う）
		List<ImportQuestionMetaDto> questions = importMetaDao.findQuestions(surveyId);
		List<ImportOptionMetaDto> options = importMetaDao.findOptions(surveyId);

		// display_order -> question meta
		Map<Integer, ImportQuestionMetaDto> questionByOrder = new HashMap<>();
		for (ImportQuestionMetaDto q : questions) {
			questionByOrder.put(q.getDisplayOrder(), q);
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

			// BOM除去（UTF-8-SIG対策：Excel由来でも壊れない）
			for (int i = 0; i < headers.size(); i++) {
				if (headers.get(i) != null) {
					headers.set(i, headers.get(i).replace("\uFEFF", "").trim());
				}
			}

			int respondentKeyIdx = indexOfIgnoreCase(headers, "respondent_key");
			if (respondentKeyIdx < 0) {
				result.addError("必須列 respondent_key が見つかりません");
				result.setTotalRows(0);
				return result;
			}

			// Q列（Q1,Q2,...）を display_order に変換して保持
			// colIndex -> display_order
			Map<Integer, Integer> qColIndexToOrder = new LinkedHashMap<>();
			for (int i = 0; i < headers.size(); i++) {
				String h = headers.get(i);
				if (h == null)
					continue;
				Integer order = parseOrderFromHeader(h);
				if (order != null) {
					qColIndexToOrder.put(i, order);
				}
			}

			if (qColIndexToOrder.isEmpty()) {
				result.addError("Q列（Q1,Q2,...）が見つかりません");
				result.setTotalRows(0);
				return result;
			}

			// ヘッダにあるQ列が、surveyIdの設問（display_order）として存在するかチェック
			for (Integer order : qColIndexToOrder.values()) {
				if (!questionByOrder.containsKey(order)) {
					result.addError("このアンケートに存在しない設問がCSVヘッダにあります: Q" + order);
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
				if (respondentKey == null)
					respondentKey = "";
				respondentKey = respondentKey.replace("\uFEFF", "").trim();

				if (respondentKey.isBlank()) {
					// respondent_key 空なら自動採番
					respondentKey = "imp-" + UUID.randomUUID();
				}

				try {
					// 3-1) respondents & response_sessions 作成
					long respondentId = answerImportDao.upsertRespondent(surveyId, respondentKey);
					long responseId = answerImportDao.createCompletedSession(respondentId);

					// 3-2) 各設問の値を保存（Q列番号=display_order）
					for (Map.Entry<Integer, Integer> entry : qColIndexToOrder.entrySet()) {
						int colIdx = entry.getKey();
						int qOrder = entry.getValue(); // Q1 -> 1

						ImportQuestionMetaDto qmeta = questionByOrder.get(qOrder);
						if (qmeta == null)
							continue; // 念のため

						long questionId = qmeta.getQuestionId();
						String qType = qmeta.getQuestionType();

						String raw = getCol(cols, colIdx);
						if (raw == null)
							raw = "";
						raw = raw.replace("\uFEFF", "").trim();

						if (raw.isBlank()) {
							// 未回答は保存しない（空欄ありOK）
							continue;
						}

						if ("SINGLE".equalsIgnoreCase(qType)) {
							int optOrder = parseIntStrict(raw); // 選択肢のdisplay_order
							Long optionId = getOptionId(optionIdByQuestionAndOrder, questionId, optOrder);
							if (optionId == null) {
								throw new IllegalArgumentException("Q" + qOrder + " の選択肢が不正: " + raw);
							}
							answerImportDao.upsertSingle(responseId, questionId, optionId);

						} else if ("MULTI".equalsIgnoreCase(qType)) {
							List<Integer> optOrders = parseMultiOrders(raw); // "1,3"
							List<Long> optionIds = new ArrayList<>();
							for (int optOrder : optOrders) {
								Long optionId = getOptionId(optionIdByQuestionAndOrder, questionId, optOrder);
								if (optionId == null) {
									throw new IllegalArgumentException("Q" + qOrder + " の選択肢が不正: " + optOrder);
								}
								optionIds.add(optionId);
							}
							answerImportDao.upsertMulti(responseId, questionId, optionIds);

						} else if ("TEXT".equalsIgnoreCase(qType) || "NUMBER".equalsIgnoreCase(qType)) {
							// NUMBERもTEXT同様に answer_text へ保存
							answerImportDao.upsertText(responseId, questionId, raw);

						} else {
							// 未知のタイプ
							throw new IllegalArgumentException(
									"未対応のquestion_type: " + qType + " (Q" + qOrder + ")");
						}
					}

					result.setSuccessRows(result.getSuccessRows() + 1);

				} catch (Exception e) {
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
			String h = headers.get(i);
			if (h == null)
				continue;
			h = h.replace("\uFEFF", "").trim();
			if (h.equalsIgnoreCase(target))
				return i;
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

	/**
	 * CSVヘッダの "Q1" から 1 を返す（= questions.display_order）
	 */
	private static Integer parseOrderFromHeader(String header) {
		if (header == null)
			return null;
		String h = header.replace("\uFEFF", "").trim();
		Matcher m = Q_COL.matcher(h);
		if (!m.matches())
			return null;
		return Integer.parseInt(m.group(1));
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
