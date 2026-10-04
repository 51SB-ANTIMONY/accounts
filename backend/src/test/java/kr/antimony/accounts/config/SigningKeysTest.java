package kr.antimony.accounts.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SigningKeysTest {

    @DisplayName("PEM 파일의 서명 키는 몇 번을 읽어도 같은 키, 같은 kid다. (재시작해도 이전 토큰을 검증할 수 있다)")
    @Test
    void loadSameKeyEveryTime(@TempDir Path dir) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        Path pem = dir.resolve("signing-key.pem");
        Files.writeString(pem, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n");

        RSAKey first = SigningKeys.load(pem);
        RSAKey second = SigningKeys.load(pem);

        assertThat(first.getKeyID()).isEqualTo(second.getKeyID());
        assertThat(first.toRSAPublicKey()).isEqualTo(keyPair.getPublic());
        assertThat(first.isPrivate()).isTrue();
        assertThat(first.getKeyID()).isEqualTo(first.computeThumbprint().toString());
    }

    @DisplayName("파일이 없는 로컬에서는 만들 때마다 다른 키다.")
    @Test
    void generateNewKey() {
        assertThat(SigningKeys.generate().getKeyID()).isNotEqualTo(SigningKeys.generate().getKeyID());
    }
}
