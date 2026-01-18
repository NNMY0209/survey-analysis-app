package com.example.app.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.app.dao.AdminResponseDao;
import com.example.app.dao.SurveyDao;
import com.example.app.dto.AdminSurveyRowDto;
import com.example.app.dto.OptionDto;
import com.example.app.dto.QuestionDto;
import com.example.app.dto.SurveyDetailDto;

@Controller
public class AdminSurveyController {

	private final SurveyDao surveyDao;
	private final AdminResponseDao adminResponseDao;

	public AdminSurveyController(SurveyDao surveyDao, AdminResponseDao adminResponseDao) {
		this.surveyDao = surveyDao;
		this.adminResponseDao = adminResponseDao;
	}

	@GetMapping("/admin/surveys")
	public String surveys(Model model) {
		List<AdminSurveyRowDto> surveys = surveyDao.findAllWithAnswerCount();

		long nowMillis = System.currentTimeMillis();
		for (AdminSurveyRowDto s : surveys) {
			s.setDisplayStatus(calcDisplayStatus(s, nowMillis));
			s.setDisplayStatusClass(calcDisplayStatusClass(s, nowMillis));
		}

		model.addAttribute("surveys", surveys);
		return "admin/surveys";
	}

	private String calcDisplayStatus(AdminSurveyRowDto s, long nowMillis) {

		// null安全
		String status = (s.getStatus() == null) ? "" : s.getStatus().toUpperCase();

		var openAt = s.getOpenAt(); // Timestamp
		var closeAt = s.getCloseAt(); // Timestamp

		boolean afterOpen = (openAt == null || nowMillis >= openAt.getTime());
		boolean beforeClose = (closeAt == null || nowMillis < closeAt.getTime());
		boolean withinWindow = afterOpen && beforeClose;

		// 1) 終了（日時優先）
		if (closeAt != null && nowMillis >= closeAt.getTime()) {
			return "終了";
		}

		// 2) 期間内なのに CLOSED ＝手動停止
		if ("CLOSED".equals(status) && withinWindow) {
			return "停止中（手動）";
		}

		// 3) 開始前
		if (openAt != null && nowMillis < openAt.getTime()) {
			return "開始前";
		}

		// 4) 期間内だが下書き
		if ("DRAFT".equals(status) && withinWindow) {
			return "一時停止（下書き）";
		}

		// 5) 期間内なら公開中
		if (withinWindow) {
			return "公開中";
		}

		// 6) その他
		return "下書き";
	}

	private String calcDisplayStatusClass(AdminSurveyRowDto s, long nowMillis) {

		String status = (s.getStatus() == null) ? "" : s.getStatus().toUpperCase();
		var openAt = s.getOpenAt();
		var closeAt = s.getCloseAt();

		boolean afterOpen = (openAt == null || nowMillis >= openAt.getTime());
		boolean beforeClose = (closeAt == null || nowMillis < closeAt.getTime());
		boolean withinWindow = afterOpen && beforeClose;

		if (closeAt != null && nowMillis >= closeAt.getTime()) {
			return "badge-ended";
		}
		if ("CLOSED".equals(status) && withinWindow) {
			return "badge-stopped";
		}
		if (openAt != null && nowMillis < openAt.getTime()) {
			return "badge-scheduled";
		}
		if ("DRAFT".equals(status) && withinWindow) {
			return "badge-paused";
		}
		if (withinWindow) {
			return "badge-live";
		}
		return "badge-draft";
	}

	@GetMapping("/admin/surveys/{surveyId}")
	public String surveyDetail(@PathVariable long surveyId, Model model) {

		SurveyDetailDto survey = surveyDao.findDetailById(surveyId);
		List<QuestionDto> questions = surveyDao.findQuestionsBySurveyId(surveyId);
		Map<Long, List<OptionDto>> optionMap = surveyDao.findOptionsBySurveyId(surveyId);

		int answerCount = adminResponseDao.countResponsesBySurveyId(surveyId, true);

		long nowMillis = System.currentTimeMillis();

		boolean isEnded = (survey.getCloseAt() != null && nowMillis >= survey.getCloseAt().getTime());

		boolean isManualClosedDuringOpen = "CLOSED".equalsIgnoreCase(survey.getStatus())
				&& (survey.getCloseAt() == null || nowMillis < survey.getCloseAt().getTime())
				&& (survey.getOpenAt() == null || nowMillis >= survey.getOpenAt().getTime());

		// ★ ここを追加（一覧と同じ表示用ステータス）
		String displayStatus = calcDisplayStatusForDetail(survey, nowMillis);
		String displayStatusClass = calcDisplayStatusClassForDetail(survey, nowMillis);

		model.addAttribute("isEnded", isEnded);
		model.addAttribute("isManualClosedDuringOpen", isManualClosedDuringOpen);

		model.addAttribute("displayStatus", displayStatus);
		model.addAttribute("displayStatusClass", displayStatusClass);

		model.addAttribute("publishForm", survey);
		model.addAttribute("survey", survey);
		model.addAttribute("questions", questions);
		model.addAttribute("optionMap", optionMap);
		model.addAttribute("answerCount", answerCount);

		return "admin/survey-detail";
	}

	private String calcDisplayStatusForDetail(SurveyDetailDto s, long nowMillis) {

		String status = (s.getStatus() == null) ? "" : s.getStatus().toUpperCase();
		var openAt = s.getOpenAt();
		var closeAt = s.getCloseAt();

		boolean afterOpen = (openAt == null || nowMillis >= openAt.getTime());
		boolean beforeClose = (closeAt == null || nowMillis < closeAt.getTime());
		boolean withinWindow = afterOpen && beforeClose;

		if (closeAt != null && nowMillis >= closeAt.getTime()) {
			return "終了";
		}
		if ("CLOSED".equals(status) && withinWindow) {
			return "停止中（手動）";
		}
		if (openAt != null && nowMillis < openAt.getTime()) {
			return "開始前";
		}
		if ("DRAFT".equals(status) && withinWindow) {
			return "一時停止（下書き）";
		}
		if (withinWindow) {
			return "公開中";
		}
		return "下書き";
	}

	private String calcDisplayStatusClassForDetail(SurveyDetailDto s, long nowMillis) {

		String status = (s.getStatus() == null) ? "" : s.getStatus().toUpperCase();
		var openAt = s.getOpenAt();
		var closeAt = s.getCloseAt();

		boolean afterOpen = (openAt == null || nowMillis >= openAt.getTime());
		boolean beforeClose = (closeAt == null || nowMillis < closeAt.getTime());
		boolean withinWindow = afterOpen && beforeClose;

		if (closeAt != null && nowMillis >= closeAt.getTime()) {
			return "badge-ended";
		}
		if ("CLOSED".equals(status) && withinWindow) {
			return "badge-stopped";
		}
		if (openAt != null && nowMillis < openAt.getTime()) {
			return "badge-scheduled";
		}
		if ("DRAFT".equals(status) && withinWindow) {
			return "badge-paused";
		}
		if (withinWindow) {
			return "badge-live";
		}
		return "badge-draft";
	}

	// ★追加：公開設定を更新
	@PostMapping("/admin/surveys/{surveyId}/publish")
	public String updatePublish(@PathVariable long surveyId,
			@RequestParam String status,
			@RequestParam(required = false) String openAt,
			@RequestParam(required = false) String closeAt,
			Model model) {

		// ========= 1. String → Timestamp 変換 =========
		Timestamp openAtTs = null;
		Timestamp closeAtTs = null;

		if (openAt != null && !openAt.isBlank()) {
			openAtTs = Timestamp.valueOf(openAt.replace("T", " ") + ":00");
		}
		if (closeAt != null && !closeAt.isBlank()) {
			closeAtTs = Timestamp.valueOf(closeAt.replace("T", " ") + ":00");
		}

		// ========= 2. open <= close の基本バリデーション =========
		if (openAtTs != null && closeAtTs != null && openAtTs.after(closeAtTs)) {
			model.addAttribute("publishError", "開始日時は終了日時より前にしてください。");

			SurveyDetailDto survey = surveyDao.findDetailById(surveyId);
			survey.setStatus(status);
			survey.setOpenAt(openAtTs);
			survey.setCloseAt(closeAtTs);

			model.addAttribute("survey", survey);
			model.addAttribute("publishForm", survey);
			model.addAttribute("questions", surveyDao.findQuestionsBySurveyId(surveyId));
			model.addAttribute("optionMap", surveyDao.findOptionsBySurveyId(surveyId));
			model.addAttribute("answerCount",
					adminResponseDao.countResponsesBySurveyId(surveyId, true));

			return "admin/survey-detail";
		}

		// ========= 3. 現在のDB状態を取得 =========
		SurveyDetailDto current = surveyDao.findPublishSettingsById(surveyId);
		Timestamp now = new Timestamp(System.currentTimeMillis());

		// ========= 4. 更新後の closeAt を考慮した「終了判定」 =========
		Timestamp effectiveCloseAt = (closeAtTs != null ? closeAtTs : current.getCloseAt());

		boolean endedAfterUpdate = (effectiveCloseAt != null && !now.before(effectiveCloseAt));

		// ========= 5. status 正規化 & 強制ルール =========
		String normalizedStatus = (status == null || status.isBlank())
				? "DRAFT"
				: status.toUpperCase();

		// 終了済みなら必ず CLOSED（再開には closeAt の変更が必須）
		if (endedAfterUpdate) {
			normalizedStatus = "CLOSED";
		}

		// ========= 6. 更新 =========
		long updatedBy = 1L; // 管理者実装までは固定

		surveyDao.updatePublishSettings(
				surveyId,
				normalizedStatus,
				openAtTs,
				closeAtTs,
				updatedBy);

		return "redirect:/admin/surveys/" + surveyId;
	}

}
