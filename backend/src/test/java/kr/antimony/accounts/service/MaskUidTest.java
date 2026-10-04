package kr.antimony.accounts.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 아이디 찾기 결과의 아이디 가리기. 관리자의 1–2자 아이디에서도 오류 없이 가린다.
class MaskUidTest {

    @DisplayName("길이에 따라 가린다: 5자 이상은 앞뒤 2자, 3–4자는 앞뒤 1자, 1–2자는 첫 글자만")
    @Test
    void maskUid() {
        assertThat(AccountRecoveryService.maskUid("antimony01")).isEqualTo("an******01");
        assertThat(AccountRecoveryService.maskUid("abcd")).isEqualTo("a**d");
        assertThat(AccountRecoveryService.maskUid("abc")).isEqualTo("a*c");
        assertThat(AccountRecoveryService.maskUid("sb")).isEqualTo("s*");
        assertThat(AccountRecoveryService.maskUid("s")).isEqualTo("s");
    }
}
