package kr.antimony.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 아이디·비밀번호 설정 (소셜로만 가입한 회원이 처음 한 번)
public record CredentialsRequest(
        @NotBlank(message = "아이디를 입력해 주세요.")
        // 글자 종류와 최대 길이만 여기서 본다. 최소 길이(일반 회원 4자, 관리자 1자)는 AccountSettingsService가 본다.
        @Pattern(regexp = "^[a-z0-9]{1,16}$", message = "영문 소문자·숫자 4–16자로 입력해 주세요.")
        String uid,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Pattern(regexp = PasswordRule.REGEX, message = PasswordRule.MESSAGE)
        String password
) {
}
