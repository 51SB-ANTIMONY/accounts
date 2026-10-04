package kr.antimony.accounts.security;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.Collection;
import java.util.Map;

// 발급 정보(oauth2_authorization)를 DB에 저장할 때 로그인한 사용자(Authentication)를 JSON으로 바꾼다.
// Spring Security는 보안상 허용 목록에 있는 클래스만 JSON에서 되살리므로, 우리 principal 클래스를 목록에 더하고
// 어떻게 되살릴지(생성자)를 알려 준다.
public final class AccountsJackson {

    private AccountsJackson() {
    }

    public static JsonMapper jsonMapper() {
        BasicPolymorphicTypeValidator.Builder allowedTypes = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(AccountUserDetails.class)
                .allowIfSubType(AccountOAuth2User.class);
        return JsonMapper.builder()
                .addModules(SecurityJacksonModules.getModules(AccountsJackson.class.getClassLoader(), allowedTypes))
                .addMixIn(AccountUserDetails.class, AccountUserDetailsMixin.class)
                .addMixIn(AccountOAuth2User.class, AccountOAuth2UserMixin.class)
                .build();
    }

    // 필드 값만 저장하고(getter는 쓰지 않는다), 클래스 이름을 함께 저장해 같은 클래스로 되살린다.
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class AccountUserDetailsMixin {
        @JsonCreator
        AccountUserDetailsMixin(@JsonProperty("userId") Long userId,
                                @JsonProperty("uid") String uid,
                                @JsonProperty("passwordHash") String passwordHash) {
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class AccountOAuth2UserMixin {
        @JsonCreator
        AccountOAuth2UserMixin(@JsonProperty("userId") Long userId,
                               @JsonProperty("attributes") Map<String, Object> attributes,
                               @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities) {
        }
    }
}
