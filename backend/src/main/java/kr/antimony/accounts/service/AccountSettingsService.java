package kr.antimony.accounts.service;

import kr.antimony.accounts.domain.User;
import kr.antimony.accounts.dto.UidAvailabilityResponse;
import kr.antimony.accounts.exception.BadRequestException;
import kr.antimony.accounts.exception.ConflictException;
import kr.antimony.accounts.exception.DuplicateUserException;
import kr.antimony.accounts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 내 계정의 아이디·비밀번호 설정과 비밀번호 변경.
// 아이디는 비밀번호가 없는 회원(소셜로만 가입)이 비밀번호와 함께 처음 정할 때만 바꿀 수 있다.
@RequiredArgsConstructor
@Service
public class AccountSettingsService {
    private final UserRepository userRepository;
    private final SignupService signupService;
    private final PasswordEncoder passwordEncoder;

    // 아이디 중복확인. 회원가입과 같은 규칙(가입 신청 중인 아이디도 사용 중)
    @Transactional(readOnly = true)
    public UidAvailabilityResponse checkUid(String uid) {
        return signupService.checkUid(uid, null);
    }

    // 아이디·비밀번호 설정. 이미 비밀번호가 있으면 409
    @Transactional
    public void setCredentials(Long userId, String uid, String rawPassword) {
        User user = findUser(userId);
        if (user.hasPassword()) {
            throw new ConflictException("이미 아이디와 비밀번호가 설정되어 있어요.");
        }
        UidAvailabilityResponse availability = checkUid(uid);
        if (!"ok".equals(availability.status())) {
            throw new DuplicateUserException(Map.of("uid", availability.message()));
        }
        user.setCredentials(uid, passwordEncoder.encode(rawPassword));
    }

    // 비밀번호 변경. 현재 비밀번호가 틀리거나 새 비밀번호가 현재와 같으면 400, 비밀번호가 없으면 409
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!user.hasPassword()) {
            throw new ConflictException("비밀번호가 설정되지 않은 계정이에요. 아이디·비밀번호를 먼저 설정해 주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("현재 비밀번호가 맞지 않아요.");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("지금 비밀번호와 다른 비밀번호로 정해 주세요.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("로그인한 회원을 찾을 수 없습니다: " + userId));
    }
}
