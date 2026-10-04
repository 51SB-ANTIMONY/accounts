package kr.antimony.accounts.controller;

import kr.antimony.accounts.domain.Admin;
import kr.antimony.accounts.domain.AdminRole;
import kr.antimony.accounts.domain.User;
import kr.antimony.accounts.domain.VerificationPurpose;
import kr.antimony.accounts.domain.UserIdentity;
import kr.antimony.accounts.repository.AdminRepository;
import kr.antimony.accounts.repository.UserIdentityRepository;
import kr.antimony.accounts.repository.UserRepository;
import kr.antimony.accounts.support.DatabaseCleaner;
import kr.antimony.accounts.support.TestSupportConfig;
import kr.antimony.accounts.support.TestSupportConfig.CapturingMailSender;
import kr.antimony.accounts.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import jakarta.servlet.http.Cookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;

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
    @Autowired AdminRepository adminRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired DatabaseCleaner databaseCleaner;
    @Autowired CapturingMailSender mailSender;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        mailSender.clear();
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

    @DisplayName("관리자가 아닌 회원: 4자보다 짧은 아이디는 중복확인 invalid, 설정은 400(errors.uid)")
    @Test
    void shortUidRejectedForMember() throws Exception {
        User user = socialOnlyUser();

        mockMvc.perform(get("/api/me/uid-availability").param("uid", "sb").with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("invalid"));
        setCredentials(user, "sb", "pass1234!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.uid").value("영문 소문자·숫자 4–16자로 입력해 주세요."));
        assertThat(userRepository.findById(user.getId()).orElseThrow().hasPassword()).isFalse();
    }

    @DisplayName("관리자: 1–16자 아이디를 정할 수 있고 그 아이디로 로그인된다. 다른 회원이 쓰는 짧은 아이디는 taken")
    @Test
    void shortUidAllowedForAdmin() throws Exception {
        User user = socialOnlyUser();
        adminRepository.save(new Admin(user.getId(), AdminRole.ADMIN, Instant.now()));
        User other = TestUsers.localUser(userRepository, passwordEncoder, "ab", "o@antimony.kr", "다른회원", "pass1234!");

        mockMvc.perform(get("/api/me/uid-availability").param("uid", other.getUid()).with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("taken"));
        mockMvc.perform(get("/api/me/uid-availability").param("uid", "s").with(authentication(TestUsers.loggedIn(user))))
                .andExpect(jsonPath("$.status").value("ok"));

        setCredentials(user, "s", "pass1234!").andExpect(status().isNoContent());
        mockMvc.perform(formLogin("/login/password").user("s").password("pass1234!"))
                .andExpect(redirectedUrl("/account"));
    }

    @DisplayName("공동 관리자도 짧은 아이디를 정할 수 있다")
    @Test
    void shortUidAllowedForCoAdmin() throws Exception {
        User user = socialOnlyUser();
        adminRepository.save(new Admin(user.getId(), AdminRole.CO_ADMIN, Instant.now()));

        setCredentials(user, "co", "pass1234!").andExpect(status().isNoContent());
        assertThat(userRepository.findById(user.getId()).orElseThrow().getUid()).isEqualTo("co");
    }

    @DisplayName("내 계정: 관리자는 adminRole이 ADMIN, 일반 회원은 null")
    @Test
    void adminRoleInMe() throws Exception {
        User admin = socialOnlyUser();
        adminRepository.save(new Admin(admin.getId(), AdminRole.ADMIN, Instant.now()));
        User member = TestUsers.localUser(userRepository, passwordEncoder, "member01", "m@antimony.kr", "일반회원", "pass1234!");

        mockMvc.perform(get("/api/me").with(authentication(TestUsers.loggedIn(admin))))
                .andExpect(jsonPath("$.adminRole").value("ADMIN"));
        mockMvc.perform(get("/api/me").with(authentication(TestUsers.loggedIn(member))))
                .andExpect(jsonPath("$.adminRole").doesNotExist());
    }

    // 세션은 DB(Spring Session)에 저장되고 쿠키로 찾는다. 1단계 응답의 세션 쿠키를 2단계에 함께 보낸다.
    private Cookie sendEmailCode(User user, String email, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/me/email/code").with(csrf()).with(authentication(TestUsers.loggedIn(user)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getCookie("ACCOUNTS_SESSION");
    }

    private ResultActions sendEmailCodeExpecting(User user, String email) throws Exception {
        return mockMvc.perform(post("/api/me/email/code").with(csrf()).with(authentication(TestUsers.loggedIn(user)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions changeEmail(User user, Cookie session, String code) throws Exception {
        var request = put("/api/me/email").with(csrf()).with(authentication(TestUsers.loggedIn(user)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"" + code + "\"}");
        if (session != null) {
            request.cookie(session);
        }
        return mockMvc.perform(request);
    }

    @DisplayName("이메일 변경: 새 이메일로 받은 인증번호로 바뀌고, 이전 이메일로 안내가 간다")
    @Test
    void changeEmailSucceeds() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "old@antimony.kr", "테스터", "pass1234!");
        Cookie session = sendEmailCode(user, "new@antimony.kr", 200);
        assertThat(session).isNotNull();
        String code = mailSender.lastCode(VerificationPurpose.EMAIL_CHANGE, "new@antimony.kr");

        changeEmail(user, session, code).andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("new@antimony.kr");
        assertThat(mailSender.emailChangedNotice("old@antimony.kr")).isEqualTo("n***@antimony.kr");
    }

    @DisplayName("이메일 변경 1단계: 지금 이메일과 같으면 400, 다른 회원의 이메일이면 409 (errors.email)")
    @Test
    void sendEmailCodeRejected() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "me@antimony.kr", "테스터", "pass1234!");
        TestUsers.localUser(userRepository, passwordEncoder, "other01", "taken@antimony.kr", "다른회원", "pass1234!");
        sendEmailCodeExpecting(user, "me@antimony.kr")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("지금 쓰고 있는 이메일이에요."));
        sendEmailCodeExpecting(user, "taken@antimony.kr")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errors.email").value("이미 다른 계정에서 쓰는 이메일이에요."));
        sendEmailCodeExpecting(user, "not-an-email").andExpect(status().isBadRequest());
    }

    @DisplayName("이메일 변경 2단계: 인증번호가 틀리면 400(errors.code), 인증번호를 받지 않은 세션이면 400. 이메일은 그대로")
    @Test
    void changeEmailRejected() throws Exception {
        User user = TestUsers.localUser(userRepository, passwordEncoder, "tester01", "old@antimony.kr", "테스터", "pass1234!");
        Cookie session = sendEmailCode(user, "new@antimony.kr", 200);
        String code = mailSender.lastCode(VerificationPurpose.EMAIL_CHANGE, "new@antimony.kr");
        String wrong = code.equals("000000") ? "111111" : "000000";

        changeEmail(user, session, wrong)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").value("인증번호가 일치하지 않아요. 다시 확인해 주세요."));
        // 다른 세션(인증번호를 요청하지 않은 로그인)에서는 맞는 인증번호로도 바꿀 수 없다
        changeEmail(user, null, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증번호를 먼저 받아 주세요."));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail()).isEqualTo("old@antimony.kr");
        assertThat(mailSender.emailChangedNotice("old@antimony.kr")).isNull();
    }
}
