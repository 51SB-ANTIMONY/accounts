package kr.antimony.accounts.dto;

import java.time.LocalDateTime;
import java.util.List;

// 내 계정 정보
public record MeResponse(
        Long id,
        String uid,
        String email,
        String nickname,
        boolean hasPassword,
        // 관리자 역할(ADMIN, CO_ADMIN). 일반 회원은 null
        String adminRole,
        List<LinkedIdentity> identities
) {
    public record LinkedIdentity(String provider, LocalDateTime linkedAt) {
    }
}
