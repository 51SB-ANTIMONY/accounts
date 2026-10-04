package kr.antimony.accounts.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

// 토큰 서명용 RSA 키를 만든다.
// - 운영: PEM 파일(PKCS#8)에서 읽는다. 키가 바뀌지 않아야 앱(blog)이 이전에 받은 토큰을 계속 검증할 수 있다.
// - 로컬: 파일이 없으면 서버를 켤 때 새로 만든다. 재시작하면 바뀐다.
// 키 id(kid)는 공개 키로 계산한 값(JWK thumbprint)이라, 같은 키면 언제나 같은 id가 나온다.
final class SigningKeys {

    private SigningKeys() {
    }

    static RSAKey load(Path pemFile) {
        try (InputStream in = Files.newInputStream(pemFile)) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(in);
            if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
                throw new IllegalStateException("공개 키를 계산할 수 없는 RSA 개인 키입니다: " + pemFile);
            }
            // 개인 키에 들어 있는 modulus와 public exponent로 공개 키를 만든다.
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
            return toJwk(publicKey, privateKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("서명 키 파일을 읽을 수 없습니다: " + pemFile, e);
        }
    }

    static RSAKey generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return toJwk((RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey toJwk(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        try {
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
