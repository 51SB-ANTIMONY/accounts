package kr.antimony.accounts.controller;

import kr.antimony.accounts.domain.User;
import kr.antimony.accounts.domain.UserIdentity;
import kr.antimony.accounts.repository.UserIdentityRepository;
import kr.antimony.accounts.repository.UserRepository;
import kr.antimony.accounts.support.DatabaseCleaner;
import kr.antimony.accounts.support.TestSupportConfig;
import kr.antimony.accounts.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 내 계정: 아이디·비밀번호 설정(소셜로만 가입한 회원)과 비밀번호 변경
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSupportConfig.class, DatabaseCleaner.class})
class AccountSettingsApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired UserIdentityRepository userIdentityRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired DatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    private User socialOnlyUser() {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "google_1a2b3c4d", "g@gmail.com", "구글회원", null);
        userIdentityRepository.save(new UserIdentity(user, "google", "google-sub-1"));
        return user;
    }

    private ResultActions setCredentials(User user, String uid, String password) throws Exception {
        return mockMvc.perform(post("/api/me/credentials").with(csrf()).with(authentication(TestUsers.loggedIn(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"uid\":\"" + uid + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions changePassword(User user, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/me/password").with(csrf()).with(authentication(TestUsers.loggedIn(user)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + current + "\",\"newPassword\":\"" + next + "\"}"));
    }

    @DisplayName("로그인하지 않으면 세 API 모두 401")
    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/me/uid-availability").param("uid", "newid01")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/me/credentials").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"uid\":\"newid01\",\"password\":\"pass1234!\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/me/password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"a\",\"newPassword\":\"pass1234!\"}")).andExpect(status().isUnauthorized());
    }

    @DisplayName("아이디 중복확인: 형식 오류는 invalid, 다른 회원의 아이디는 taken, 그 밖에는 ok")
    @Test
    void uidAvailability() throws Exception {
        User user = socialOnlyUser();
        TestUsers.localUser(userRepository, passwordEncoder, "taken01", "t@antimony.kr", "다른회원", "pass1234!");

        mockMvc.perform(get("/api/me/uid-availability").param("uid", "AB").with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("invalid"));
        mockMvc.perform(get("/api/me/uid-availability").param("uid", "taken01").with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("taken"));
        mockMvc.perform(get("/api/me/uid-availability").param("uid", "newid01").with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @DisplayName("아이디·비밀번호 설정: 소셜 회원의 아이디와 비밀번호가 바뀌고, 새 아이디·비밀번호로 로그인되며, Google 연결을 해제할 수 있다")
    @Test
    void setCredentialsForSocialUser() throws Exception {
        User user = socialOnlyUser();

        setCredentials(user, "newid01", "pass1234!").andExpect(status().isNoContent());

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getUid()).isEqualTo("newid01");
        assertThat(passwordEncoder.matches("pass1234!", saved.getPassword())).isTrue();

        mockMvc.perform(formLogin("/login/password").user("newid01").password("pass1234!"))
                .andExpect(redirectedUrl("/account"));

        mockMvc.perform(delete("/api/me/identities/google").with(csrf()).with(authentication(TestUsers.loggedIn(saved))))
                .andExpect(status().isNoContent());
        assertThat(userIdentityRepository.findByUserId(user.getId())).isEmpty();
    }

    @DisplayName("아이디·비밀번호 설정: 이미 비밀번호가 있으면 409, 아이디가 바뀌지 않는다")
    @Test
    void setCredentialsRejectedWhenPasswordExists() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "a@antimony.kr", "테스터", "pass1234!");

        setCredentials(user, "newid01", "newpass12!")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 아이디와 비밀번호가 설정되어 있어요."));
        assertThat(userRepository.findById(user.getId()).orElseThrow().getUid()).isEqualTo("tester01");
    }

    @DisplayName("아이디·비밀번호 설정: 사용 중인 아이디는 409(errors.uid), 형식이 틀린 값은 400")
    @Test
    void setCredentialsValidation() throws Exception {
        User user = socialOnlyUser();
        TestUsers.localUser(userRepository, passwordEncoder, "taken01", "t@antimony.kr", "다른회원", "pass1234!");

        setCredentials(user, "taken01", "pass1234!")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.uid").value("이미 사용 중인 아이디예요."));
        setCredentials(user, "Bad_ID", "pass1234!").andExpect(status().isBadRequest());
        setCredentials(user, "newid01", "short").andExpect(status().isBadRequest());

        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getUid()).isEqualTo("google_1a2b3c4d");
        assertThat(saved.hasPassword()).isFalse();
    }

    @DisplayName("비밀번호 변경: 성공하면 새 비밀번호로만 로그인된다")
    @Test
    void changePasswordSucceeds() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "a@antimony.kr", "테스터", "pass1234!");

        changePassword(user, "pass1234!", "newpass12!").andExpect(status().isNoContent());

        mockMvc.perform(formLogin("/login/password").user("tester01").password("newpass12!"))
                .andExpect(redirectedUrl("/account"));
        mockMvc.perform(formLogin("/login/password").user("tester01").password("pass1234!"))
                .andExpect(redirectedUrl("/login?error=bad_credentials"));
    }

    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리거나 새 비밀번호가 같으면 400, 비밀번호가 없는 회원은 409")
    @Test
    void changePasswordRejected() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "a@antimony.kr", "테스터", "pass1234!");

        changePassword(user, "wrong1234!", "newpass12!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("현재 비밀번호가 맞지 않아요."));
        changePassword(user, "pass1234!", "pass1234!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("지금 비밀번호와 다른 비밀번호로 정해 주세요."));
        changePassword(user, "pass1234!", "short").andExpect(status().isBadRequest());
        assertThat(passwordEncoder.matches("pass1234!", userRepository.findById(user.getId()).orElseThrow().getPassword())).isTrue();

        changePassword(socialOnlyUser(), "anything1!", "newpass12!")
                .andExpect(status().isConflict());
    }
}
