package kr.antimony.accounts.controller;

import jakarta.validation.Valid;
import kr.antimony.accounts.dto.CredentialsRequest;
import kr.antimony.accounts.dto.MeResponse;
import kr.antimony.accounts.dto.PasswordChangeRequest;
import kr.antimony.accounts.dto.UidAvailabilityResponse;
import kr.antimony.accounts.security.AccountPrincipal;
import kr.antimony.accounts.service.AccountSettingsService;
import kr.antimony.accounts.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 로그인한 회원의 계정 정보. 로그인 방식(아이디·비밀번호, 소셜)과 상관없이 principal에서 회원 id를 꺼낸다.
@RequiredArgsConstructor
@RestController
public class MeApiController {
    private final UserService userService;
    private final AccountSettingsService accountSettingsService;

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal AccountPrincipal principal) {
        return userService.me(principal.getUserId());
    }

    // 소셜 계정 연결 해제
    @DeleteMapping("/api/me/identities/{provider}")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable String provider) {
        userService.unlinkSocial(principal.getUserId(), provider);
        return ResponseEntity.noContent().build();
    }

    // 아이디·비밀번호 설정 화면의 아이디 중복확인
    @GetMapping("/api/me/uid-availability")
    public UidAvailabilityResponse checkUid(@AuthenticationPrincipal AccountPrincipal principal, @RequestParam String uid) {
        return accountSettingsService.checkUid(principal.getUserId(), uid);
    }

    // 아이디·비밀번호 설정 (소셜로만 가입한 회원)
    @PostMapping("/api/me/credentials")
    public ResponseEntity<Void> setCredentials(@AuthenticationPrincipal AccountPrincipal principal,
                                               @Valid @RequestBody CredentialsRequest request) {
        accountSettingsService.setCredentials(principal.getUserId(), request.uid(), request.password());
        return ResponseEntity.noContent().build();
    }

    // 비밀번호 변경
    @PutMapping("/api/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AccountPrincipal principal,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        accountSettingsService.changePassword(principal.getUserId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
