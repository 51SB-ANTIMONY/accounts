package kr.antimony.accounts.controller;

import java.util.Map;
import kr.antimony.accounts.security.AccountPrincipal;
import kr.antimony.accounts.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SessionController {
    private final UserService userService;

    @GetMapping("/api/session")
    public ResponseEntity<Map<String, String>> session(
            @AuthenticationPrincipal AccountPrincipal principal, CsrfToken csrfToken) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "nickname", userService.me(principal.getUserId()).nickname(),
                "token", csrfToken.getToken(),
                "parameterName", csrfToken.getParameterName()));
    }
}
