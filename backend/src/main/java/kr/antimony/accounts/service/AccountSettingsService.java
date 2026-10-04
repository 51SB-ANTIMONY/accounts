package kr.antimony.accounts.service;

import kr.antimony.accounts.domain.User;
import kr.antimony.accounts.dto.UidAvailabilityResponse;
import kr.antimony.accounts.exception.BadRequestException;
import kr.antimony.accounts.exception.ConflictException;
import kr.antimony.accounts.exception.DuplicateUserException;
import kr.antimony.accounts.exception.InvalidInputException;
import kr.antimony.accounts.repository.AdminRepository;
import kr.antimony.accounts.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

// 내 계정의 아이디·비밀번호 설정과 비밀번호 변경.
// 아이디는 비밀번호가 없는 회원(소셜로만 가입)이 비밀번호와 함께 처음 정할 때만 바꿀 수 있다.
// 관리자(admins 테이블에 있는 회원)는 4자보다 짧은 아이디(1–16자)도 정할 수 있다.
@RequiredArgsConstructor
@Service
public class AccountSettingsService {
    private static final String ADMIN_UID_PATTERN = "^[a-z0-9]{1,16}$";

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final SignupService signupService;
    private final PasswordEncoder passwordEncoder;

    // 아이디 중복확인. 회원가입과 같은 규칙(가입 신청 중인 아이디도 사용 중)이고, 관리자만 짧은 아이디를 허용한다.
    @Transactional(readOnly = true)
    public UidAvailabilityResponse checkUid(Long userId, String uid) {
        boolean shortUid = uid != null && uid.matches(ADMIN_UID_PATTERN) && uid.length() < 4;
        if (shortUid && adminRepository.existsById(userId)) {
            // 4자보다 짧은 아이디는 가입 신청에 있을 수 없으므로 회원 아이디만 확인한다.
            return userRepository.existsByUid(uid)
                    ? new UidAvailabilityResponse("taken", "이미 사용 중인 아이디예요.")
                    : new UidAvailabilityResponse("ok", "사용 가능한 아이디예요.");
        }
        return signupService.checkUid(uid, null);
    }

    // 아이디·비밀번호 설정. 이미 비밀번호가 있으면 409
    @Transactional
    public void setCredentials(Long userId, String uid, String rawPassword) {
        User user = findUser(userId);
        if (user.hasPassword()) {
            throw new ConflictException("이미 아이디와 비밀번호가 설정되어 있어요.");
        }
        UidAvailabilityResponse availability = checkUid(userId, uid);
        if ("invalid".equals(availability.status())) {
            throw new InvalidInputException(Map.of("uid", availability.message()));
        }
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
