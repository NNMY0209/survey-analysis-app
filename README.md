# survey-analysis-app

アンケート集計システム（実習課題）

## 概要
Spring Boot + JDBC + MySQL を用いたアンケート集計システムです。
管理者向けにアンケート結果の確認・集計を行うことを目的としています。

## 使用技術
- Java 17
- Spring Boot 3.5.9
- Spring JDBC (JdbcTemplate)
- MySQL
- Gradle

## 現在の実装状況
- Spring Boot プロジェクト初期構築
- MySQL 接続確認（/dbcheck）
- GitHub リポジトリ管理（Fork運用）

## 起動方法
1. MySQL に `survey_app` データベースを作成
2. `application-local.properties` にDB接続情報を設定
3. Spring Boot アプリケーションを起動
4. ブラウザで `http://localhost:8080/dbcheck` にアクセス

## CSVインポート仕様

本システムでは、アンケートの **回答データ** および **設問定義** を CSV ファイルから一括登録できます。  
CSVは Excel で作成された UTF-8(BOM付き) ファイルにも対応しています。

---

## CSVインポート仕様（設問定義 / 回答）

本システムでは、アンケートの **設問定義** および **回答データ** を CSV から一括登録できます。
- Excel で作成した CSV（UTF-8 / UTF-8 BOM 付き）に対応
- 設問は **display_order（表示順）** をキーに識別します

---

### 設問定義インポート（2ファイル）

#### questions.csv
```csv
survey_id,display_order,question_text,question_type,question_role,is_reverse,is_required,scales
question_type : SINGLE_CHOICE / MULTI_CHOICE / TEXT / NUMBER

question_role : NORMAL / ATTENTION_CHECK / VALIDITY_CHECK

is_reverse : 0/1（逆転項目）

is_required : 0/1（必須）

scales : 下位尺度と重みづけ（scale_code:weight|scale_code:weight）

例）DEP:1.0|ANX:0.5

weight 省略時は 1.0 として扱われます

scale_code は scales.scale_code に存在する必要があります

question_options.csv
csv
コードをコピーする
survey_id,question_display_order,display_order,option_text,score,is_correct
question_display_order : 対応する設問の display_order（例：Q1なら 1）

display_order : 選択肢の表示順（回答CSVではこの番号を指定）

score : 集計用スコア

is_correct : 注意確認用の期待回答（通常は 0、ATTENTION_CHECK で利用）

インポート時の挙動
設問は (survey_id, display_order) をキーに 上書き更新（upsert）

question_options と scale_questions は、対象設問について 全削除 → 再登録

回答が存在するアンケートには、設問定義インポートはできません

回答データインポート
CSVヘッダ例
csv
コードをコピーする
respondent_key,Q1,Q2,Q3,...
Qn は設問の display_order = n を意味します

設問タイプ	CSVの値	意味
SINGLE_CHOICE	2	選択肢の display_order
MULTI_CHOICE	1,3	複数選択（display_order をカンマ区切り）
TEXT	任意文字列	自由記述
NUMBER	数値	数値（文字列として保存）

※ 空欄は未回答として扱われ、保存されません。

サンプルCSV
docs/sample_questions.csv

docs/sample_question_options.csv

docs/sample_responses.csv