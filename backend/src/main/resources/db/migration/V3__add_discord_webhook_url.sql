-- ============================================
-- V3__add_discord_webhook_url.sql
-- notification_settings 테이블에 Discord Webhook URL 컬럼 추가
-- ============================================

ALTER TABLE notification_settings
    ADD COLUMN discord_webhook_url VARCHAR(500);
