package kr.antimony.accounts.security;

import kr.antimony.accounts.domain.User;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

// 아이디·비밀번호 로그인에 성공했을 때의 principal. 세션에 저장되므로 엔티티 대신 필요한 값만 담는다.
// 세션과 발급 정보(oauth2_authorization)에 저장되므로, 로그인이 끝나면 비밀번호 해시를 지운다(CredentialsContainer).
@Getter
public class AccountUserDetails implements UserDetails, CredentialsContainer, AccountPrincipal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String uid;
    private String passwordHash;

    public AccountUserDetails(User user) {
        this(user.getId(), user.getUid(), user.getPassword());
    }

    // DB에 저장한 발급 정보(JSON)에서 되살릴 때 쓴다 (AccountsJackson)
    public AccountUserDetails(Long userId, String uid, String passwordHash) {
        this.userId = userId;
        this.uid = uid;
        this.passwordHash = passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return uid;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    // 로그인 처리(ProviderManager)가 인증을 마친 뒤 호출한다.
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
