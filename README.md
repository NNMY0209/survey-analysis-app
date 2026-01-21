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

### 回答データインポート（responses）

#### CSVヘッダ例
```csv
respondent_key,Q1,Q2,Q3,Q4
列	内容
respondent_key	回答者識別キー（空欄可／空欄時は自動採番）
Qn	設問の表示順（display_order = n）
入力ルール
設問タイプ	入力値	意味
SINGLE_CHOICE	2	選択肢の display_order
MULTI_CHOICE	1,3	複数選択（display_order をカンマ区切り）
TEXT	任意文字列	自由記述
NUMBER	数値	数値（文字列として保存）

※ 空欄は未回答として扱われます。

設問定義インポート

設問定義は 2ファイル構成 でインポートします。

questions.csv
survey_id,display_order,question_text,question_type,question_role,is_reverse,is_required

列	内容
survey_id	アンケートID
display_order	設問の表示順
question_text	設問文
question_type	SINGLE_CHOICE / MULTI_CHOICE / TEXT / NUMBER
question_role	NORMAL / ATTENTION_CHECK / VALIDITY_CHECK
is_reverse	0:通常 / 1:逆転項目
is_required	0:任意 / 1:必須
question_options.csv
survey_id,question_display_order,display_order,option_text,score,is_correct

列	内容
question_display_order	対応する設問の表示順
display_order	選択肢の表示順
option_text	選択肢文
score	集計用スコア
is_correct	0/1（注意確認用の期待回答）
インポート時の仕様

設問は (survey_id, display_order) で一意に識別されます

同一キーの設問が存在する場合は 上書き更新 されます

選択肢は対象設問ごとに 全削除 → 再登録 されます

回答が存在するアンケートには、設問定義のインポートはできません

サンプルCSV

回答データ例：docs/sample_responses.csv

設問定義例：docs/sample_questions.csv

選択肢定義例：docs/sample_question_options.csv


## 回答データ CSV インポート仕様

本システムでは、アンケート回答を CSV ファイルから一括登録できます。

- Excel で作成した CSV（UTF-8 / UTF-8 BOM 付き）に対応
- 各設問は **display_order（表示順）** を用いて識別されます

---

### CSVヘッダ形式

```csv
respondent_key,Q1,Q2,Q3,Q4,...
列名	内容
respondent_key	回答者識別キー（空欄可）
Qn	設問の表示順（display_order = n）

respondent_key が空欄の場合、自動的に一意なキーが採番されます

Q列はアンケートに定義されている設問数に応じて可変です

設問タイプ別の入力ルール
設問タイプ	CSVの値	意味
SINGLE_CHOICE	2	選択肢の display_order
MULTI_CHOICE	1,3	複数選択（display_order をカンマ区切り）
TEXT	任意の文字列	自由記述回答
NUMBER	数値	数値回答（文字列として保存）

記入例
csv

respondent_key,Q1,Q2,Q3,Q4
user001,5,"1,3","とても満足しています",42
user002,4,2,"普通です",30
未回答の扱い
セルが空欄の場合、その設問は 未回答 として扱われます

未回答の設問は DB に保存されません

インポート時の仕様
CSVヘッダに指定された Q列が、対象アンケートの設問（display_order）として存在しない場合はエラーになります

各行は 1回答単位 で処理されます

行単位でエラーが発生した場合、その行のみスキップされ、他の行の処理は継続されます

回答は「完了済み回答（COMPLETED）」として登録されます

サンプルCSV
回答データ例：docs/sample_responses.csv

