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

## 今後の予定
- アンケート一覧画面の作成
- 設問・回答データの表示
- 集計機能の実装
