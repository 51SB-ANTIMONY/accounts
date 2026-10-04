package kr.antimony.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 아이디·비밀번호 설정 (소셜로만 가입한 회원이 처음 한 번)
public record CredentialsRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        @Pattern(regexp = "^[a-z0-9]{4,16}$", message = "영문 소문자·숫자 4–16자로 입력해 주세요.")
        String uid,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = PasswordRule.REGEX, message = PasswordRule.MESSAGE)
        String password
) {
}
