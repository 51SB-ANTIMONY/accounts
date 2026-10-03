-- accounts 첫 테이블 (1단계 ~ 3단계까지의 엔티티)
-- MySQL과 H2(MySQL 호환 모드, 로컬·테스트)에서 모두 실행되도록 표준 문법으로 쓴다.

-- 회원
CREATE TABLE users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    uid              VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password         VARCHAR(255),
    nickname         VARCHAR(255) NOT NULL,
    marketing_agreed BIT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_uid UNIQUE (uid),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_nickname UNIQUE (nickname)
);

-- 회원에 연결된 소셜 계정 (제공자의 sub로 구별)
CREATE TABLE user_identities (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(255) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    created_at       DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_identities_provider_subject UNIQUE (provider, provider_subject),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 이메일 인증번호 (가입, 아이디 찾기, 비밀번호 재설정)
CREATE TABLE email_verifications (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    purpose         VARCHAR(20)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    code_hash       VARCHAR(255) NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    failed_attempts INTEGER      NOT NULL,
    verified_at     DATETIME(6),
    reset_token     VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_email_verifications_purpose_email UNIQUE (purpose, email),
    CONSTRAINT uk_email_verifications_reset_token UNIQUE (reset_token)
);

-- 인증 전의 가입 신청
CREATE TABLE pending_signups (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255) NOT NULL,
    uid              VARCHAR(255) NOT NULL,
    nickname         VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    marketing_agreed BIT          NOT NULL,
    requested_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pending_signups_email UNIQUE (email)
);
