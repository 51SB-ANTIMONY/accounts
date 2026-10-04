package kr.antimony.accounts.mail;

import kr.antimony.accounts.domain.VerificationPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// spring.mail.host 값이 있을 때만 쓰인다. 실제 메일을 보낸다.
// (운영 설정은 SMTP_HOST가 없으면 빈 값이 되므로, "설정됨"이 아니라 "값이 비어 있지 않음"으로 판단한다)
@Component
@ConditionalOnExpression("!'${spring.mail.host:}'.isEmpty()")
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender mailSender;
    // 보내는 사람 주소. 비워 두면 SMTP 로그인 계정으로 보낸다(Gmail). 다른 메일 서비스는 정해 둔 주소가 필요하다.
    private final String from;

    public SmtpVerificationMailSender(JavaMailSender mailSender, @Value("${accounts.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String email, VerificationPurpose purpose, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject("[ANTIMONY] " + subject(purpose));
        message.setText("인증번호: " + code + "\n\n3분 안에 입력해 주세요. 요청하지 않으셨다면 이 메일을 무시해 주세요.");
        mailSender.send(message);
    }

    @Override
    public void sendEmailChanged(String oldEmail, String newEmailMasked) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(oldEmail);
        message.setSubject("[ANTIMONY] 계정 이메일이 바뀌었어요");
        message.setText("ANTIMONY 계정의 이메일이 " + newEmailMasked + "(으)로 바뀌었어요.\n\n"
                + "직접 바꾸지 않으셨다면 계정이 다른 사람에게 사용되고 있을 수 있어요. 바로 비밀번호를 바꾸거나 운영자에게 알려 주세요.");
        mailSender.send(message);
    }

    private static String subject(VerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "회원가입 이메일 인증번호";
            case FIND_ID -> "아이디 찾기 인증번호";
            case RESET_PASSWORD -> "비밀번호 재설정 인증번호";
            case EMAIL_CHANGE -> "이메일 변경 인증번호";
        };
    }
}
