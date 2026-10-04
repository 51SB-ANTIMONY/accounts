package kr.antimony.accounts.domain;

// 관리자 역할. 일반 회원은 역할이 없다(admins 테이블에 없음).
public enum AdminRole {
    ADMIN,      // 관리자
    CO_ADMIN    // 공동 관리자
}
