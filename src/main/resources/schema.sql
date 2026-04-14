-- 1. USER 테이블
CREATE TABLE IF NOT EXISTS users (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "email" VARCHAR(255) NOT NULL UNIQUE,
    "nickname" VARCHAR(100) NOT NULL,
    "password" VARCHAR(255) NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE
);

-- 2. ARTICLE 테이블
CREATE TABLE IF NOT EXISTS articles (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "source" VARCHAR(255) NOT NULL,
    "sourceUrl" VARCHAR(500) NOT NULL,
    "title" VARCHAR(255) NOT NULL,
    "publishDate" TIMESTAMP NOT NULL,
    "summary" TEXT,
    "commentCount" BIGINT DEFAULT 0,
    "viewCount" BIGINT DEFAULT 0,
    "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. INTEREST 테이블
CREATE TABLE IF NOT EXISTS interests (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,
    "keywords" TEXT,
    "subscriberCount" BIGINT DEFAULT 0
);

-- 4. SUBSCRIPTION 테이블
CREATE TABLE IF NOT EXISTS subscriptions (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "userId" UUID NOT NULL,
    "interestId" UUID NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_sub_user" FOREIGN KEY ("userId") REFERENCES users ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_sub_interest" FOREIGN KEY ("interestId") REFERENCES interests ("id") ON DELETE CASCADE
);

-- 5. COMMENT 테이블
CREATE TABLE IF NOT EXISTS comments (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "articleId" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "content" TEXT NOT NULL,
    "likeCount" BIGINT DEFAULT 0,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "isDeleted" BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT "fk_comment_article" FOREIGN KEY ("articleId") REFERENCES articles ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_comment_user" FOREIGN KEY ("userId") REFERENCES users ("id") ON DELETE CASCADE
);

-- 6. COMMENT_LIKE 테이블
CREATE TABLE IF NOT EXISTS comment_likes (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "commentId" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_like_comment" FOREIGN KEY ("commentId") REFERENCES comments ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_like_user" FOREIGN KEY ("userId") REFERENCES users ("id") ON DELETE CASCADE
);

-- 7. ARTICLE_VIEW 테이블
CREATE TABLE IF NOT EXISTS ARTICLE_VIEWS (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "articleId" UUID NOT NULL,
    "userId" UUID NOT NULL,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_view_article" FOREIGN KEY ("articleId") REFERENCES articles ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_view_user" FOREIGN KEY ("userId") REFERENCES users ("id") ON DELETE CASCADE
);

-- 8. NOTIFICATION 테이블
CREATE TABLE IF NOT EXISTS NOTIFICATIONS (
    "id" UUID DEFAULT random_uuid() PRIMARY KEY,
    "userId" UUID NOT NULL,
    "content" VARCHAR(255) NOT NULL,
    "resourceType" VARCHAR(255) NOT NULL,
    "resourceId" UUID NOT NULL,
    "confirmed" BOOLEAN NOT NULL DEFAULT FALSE,
    "createdAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_noti_user" FOREIGN KEY ("userId") REFERENCES users ("id") ON DELETE CASCADE
);