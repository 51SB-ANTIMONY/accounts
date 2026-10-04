package kr.antimony.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 인증번호만 (이메일은 서버 세션에 있는 경우)
public record CodeRequest(
        @NotBlank(message = "인증번호 6자리를 입력해 주세요.")
        @Pattern(regexp = "^\\d{6}$", message = "인증번호 6자리를 입력해 주세요.")
        String code
) {
}
