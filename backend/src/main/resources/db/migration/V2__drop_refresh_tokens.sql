-- ============================================
-- V2__drop_refresh_tokens.sql
-- Refresh Token 저장소를 Redis로 이전
-- ============================================

DROP INDEX IF EXISTS idx_refresh_tokens_user;
DROP INDEX IF EXISTS idx_refresh_tokens_token;
DROP TABLE IF EXISTS refresh_tokens;
