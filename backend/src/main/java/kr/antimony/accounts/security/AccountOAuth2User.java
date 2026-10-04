package kr.antimony.accounts.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// 소셜 로그인에 성공했을 때의 principal. 제공자가 준 정보와 함께, 연결된 우리 회원 id를 담는다.
// getName()이 제공자의 sub가 아니라 우리 회원 id를 돌려준다.
// 세션과 발급 정보(oauth2_authorization, JSON)에 저장되므로 값은 일반 컬렉션(LinkedHashMap, ArrayList)에 담는다.
@Getter
public class AccountOAuth2User implements OAuth2User, AccountPrincipal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final Map<String, Object> attributes;
    private final Collection<GrantedAuthority> authorities;

    public AccountOAuth2User(Long userId, Map<String, Object> attributes, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.attributes = new LinkedHashMap<>(attributes);
        this.authorities = new ArrayList<>(authorities);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.unmodifiableCollection(authorities);
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
