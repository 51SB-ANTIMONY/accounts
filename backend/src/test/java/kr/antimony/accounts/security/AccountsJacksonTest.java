package kr.antimony.accounts.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 발급 정보(oauth2_authorization)에 저장되는 로그인 사용자를 JSON으로 바꿨다가 되살릴 수 있는지 확인한다.
// JdbcOAuth2AuthorizationService는 attributes를 Map<String, Object>로 저장하고 읽는다.
class AccountsJacksonTest {

    private final JsonMapper jsonMapper = AccountsJackson.jsonMapper();

    private Object roundTrip(Object principal) {
        // SAS와 같이 일반 HashMap에 담아 저장한다. (Map.of로 만든 변경 불가 Map은 클래스 정보가 저장되지 않는다)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(Principal.class.getName(), principal);
        String json = jsonMapper.writeValueAsString(attributes);
        Map<String, Object> restored = jsonMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        return restored.get(Principal.class.getName());
    }

    @DisplayName("아이디·비밀번호 로그인: AccountUserDetails를 되살린다. 지운 비밀번호 해시는 저장되지 않는다.")
    @Test
    void accountUserDetails() {
        AccountUserDetails details = new AccountUserDetails(7L, "tester01", "{bcrypt}hash");
        details.eraseCredentials();
        var token = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());

        Object restored = roundTrip(token);

        assertThat(restored).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        AccountUserDetails principal = (AccountUserDetails) ((UsernamePasswordAuthenticationToken) restored).getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(7L);
        assertThat(principal.getUid()).isEqualTo("tester01");
        assertThat(principal.getPasswordHash()).isNull();
    }

    @DisplayName("소셜 로그인: AccountOAuth2User를 제공자 정보, 권한과 함께 되살린다.")
    @Test
    void accountOAuth2User() {
        Map<String, Object> attributes = Map.of("sub", "google-123", "email", "a@gmail.com", "email_verified", true);
        AccountOAuth2User user = new AccountOAuth2User(7L, attributes,
                List.of(new OAuth2UserAuthority(attributes), new SimpleGrantedAuthority("ROLE_USER")));
        var token = new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");

        Object restored = roundTrip(token);

        assertThat(restored).isInstanceOf(OAuth2AuthenticationToken.class);
        AccountOAuth2User principal = (AccountOAuth2User) ((OAuth2AuthenticationToken) restored).getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(7L);
        assertThat(principal.getName()).isEqualTo("7");
        assertThat(principal.getAttributes()).containsEntry("email", "a@gmail.com").containsEntry("email_verified", true);
        assertThat(principal.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }
}
