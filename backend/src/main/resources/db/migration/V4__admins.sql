-- 관리자. 일반 회원은 이 테이블에 없다. 행이 있으면 그 역할(ADMIN: 관리자, CO_ADMIN: 공동 관리자)을 가진다.
CREATE TABLE admins (
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_admins_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 운영 DB의 1번 회원(서비스 운영자)을 관리자로. Flyway는 이 파일을 한 번만 실행한다.
-- 회원이 없는 DB(로컬·테스트)에서는 아무 행도 들어가지 않는다.
INSERT INTO admins (user_id, role, created_at)
SELECT id, 'ADMIN', CURRENT_TIMESTAMP(6) FROM users WHERE id = 1;
