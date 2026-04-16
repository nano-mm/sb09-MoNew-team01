-- 기존 테스트 데이터 정리
DELETE FROM comment_likes;
DELETE FROM comments;
DELETE FROM subscriptions;
DELETE FROM notifications;
DELETE FROM interests;
DELETE FROM articles;
DELETE FROM users;

-- notifications 컬럼 네이밍 보정(schema.sql 미수정 조건 충족)
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS resource_type VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- 테스트 사용자 생성
INSERT INTO users (id, email, nickname, password, created_at, is_deleted) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'test@test.com', 'testuser', 'password123', CURRENT_TIMESTAMP, FALSE);

-- 테스트 관심사 생성
INSERT INTO interests (id, name, keywords, subscriber_count) VALUES
('550e8400-e29b-41d4-a716-446655440010', '스포츠', '스포츠,축구,야구', 100),
('550e8400-e29b-41d4-a716-446655440011', '정치', '정치,선거,정책', 150),
('550e8400-e29b-41d4-a716-446655440012', '경제', '경제,주식,금융', 200);

-- 테스트 알림 생성 (기존 컬럼 + JPA snake_case 컬럼 동시 반영)
INSERT INTO notifications (
  id,
  user_id,
  content,
  resourceType,
  resource_type,
  resource_id,
  confirmed,
  createdAt,
  created_at,
  updatedAt,
  updated_at
) VALUES
('550e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440000', '[스포츠]와 관련된 기사가 5건 등록되었습니다.', 'INTEREST', 'INTEREST', '550e8400-e29b-41d4-a716-446655440010', FALSE, DATEADD('SECOND', -30, CURRENT_TIMESTAMP), DATEADD('SECOND', -30, CURRENT_TIMESTAMP), DATEADD('SECOND', -30, CURRENT_TIMESTAMP), DATEADD('SECOND', -30, CURRENT_TIMESTAMP)),
('550e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440000', '[정치]와 관련된 기사가 3건 등록되었습니다.', 'INTEREST', 'INTEREST', '550e8400-e29b-41d4-a716-446655440011', FALSE, DATEADD('SECOND', -20, CURRENT_TIMESTAMP), DATEADD('SECOND', -20, CURRENT_TIMESTAMP), DATEADD('SECOND', -20, CURRENT_TIMESTAMP), DATEADD('SECOND', -20, CURRENT_TIMESTAMP)),
('550e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440000', '[경제]와 관련된 기사가 7건 등록되었습니다.', 'INTEREST', 'INTEREST', '550e8400-e29b-41d4-a716-446655440012', FALSE, DATEADD('SECOND', -10, CURRENT_TIMESTAMP), DATEADD('SECOND', -10, CURRENT_TIMESTAMP), DATEADD('SECOND', -10, CURRENT_TIMESTAMP), DATEADD('SECOND', -10, CURRENT_TIMESTAMP));
