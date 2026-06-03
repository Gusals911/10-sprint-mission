package com.sprint.mission.discodeit.config.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sprint.mission.discodeit.entity.Role;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String USERNAME_CLAIM = "username";
    private static final String ROLE_CLAIM = "role";
    private static final String ISSUER = "discodeit";

    @Getter
    @Value("${jwt.key}")
    private String secretKey;

    @Getter
    @Value("${jwt.access-token-expiration-minutes}")
    private int accessTokenExpirationMinutes;

    @Getter
    @Value("${jwt.refresh-token-expiration-minutes}")
    private int refreshTokenExpirationMinutes;

    public String generateAccessToken(DiscodeitUserDetails userDetails) {
        return generateToken(userDetails, ACCESS_TOKEN_TYPE, accessTokenExpirationMinutes);
    }

    public String generateRefreshToken(DiscodeitUserDetails userDetails) {
        return generateToken(userDetails, REFRESH_TOKEN_TYPE, refreshTokenExpirationMinutes);
    }

    public String generateAccessToken(UUID userId, String username, Role role) {
        return generateToken(userId, username, role, ACCESS_TOKEN_TYPE, accessTokenExpirationMinutes);
    }

    public String generateRefreshToken(UUID userId, String username, Role role) {
        return generateToken(userId, username, role, REFRESH_TOKEN_TYPE, refreshTokenExpirationMinutes);
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE);
    }

    public Map<String, Object> getClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("JWT 서명 검증에 실패했습니다.");
            }

            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            throw new RuntimeException("JWT 파싱에 실패했습니다.", e);
        }
    }

    public UUID getUserId(String token) {
        try {
            return UUID.fromString(getJwtClaimsSet(token).getSubject());
        } catch (Exception e) {
            throw new RuntimeException("JWT에서 사용자 ID를 읽을 수 없습니다.", e);
        }
    }

    public String getUsername(String token) {
        try {
            return getJwtClaimsSet(token).getStringClaim(USERNAME_CLAIM);
        } catch (Exception e) {
            throw new RuntimeException("JWT에서 사용자 이름을 읽을 수 없습니다.", e);
        }
    }

    public Role getRole(String token) {
        try {
            String role = getJwtClaimsSet(token).getStringClaim(ROLE_CLAIM);
            return Role.valueOf(role);
        } catch (Exception e) {
            throw new RuntimeException("JWT에서 사용자 권한을 읽을 수 없습니다.", e);
        }
    }

    public Date getExpirationTime(String token) {
        try {
            return getJwtClaimsSet(token).getExpirationTime();
        } catch (Exception e) {
            throw new RuntimeException("JWT 만료 시간을 읽을 수 없습니다.", e);
        }
    }

    private String generateToken(
            DiscodeitUserDetails userDetails,
            String tokenType,
            int expirationMinutes
    ) {
        return generateToken(
                userDetails.getUserDto().id(),
                userDetails.getUsername(),
                userDetails.getUserDto().role(),
                tokenType,
                expirationMinutes
        );
    }

    private String generateToken(
            UUID userId,
            String username,
            Role role,
            String tokenType,
            int expirationMinutes
    ) {
        try {
            JWSSigner signer = new MACSigner(secretKey.getBytes(StandardCharsets.UTF_8));

            Date now = new Date();
            Date expiration = new Date(now.getTime() + expirationMinutes * 60L * 1000L);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim(USERNAME_CLAIM, username)
                    .claim(ROLE_CLAIM, role.name())
                    .claim(TOKEN_TYPE_CLAIM, tokenType)
                    .issuer(ISSUER)
                    .issueTime(now)
                    .expirationTime(expiration)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("JWT 발급에 실패했습니다.", e);
        }
    }

    private boolean validateToken(String token, String expectedTokenType) {
        try {
            JWTClaimsSet claimsSet = getJwtClaimsSet(token);

            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return false;
            }

            String tokenType = claimsSet.getStringClaim(TOKEN_TYPE_CLAIM);
            return expectedTokenType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    private JWTClaimsSet getJwtClaimsSet(String token) throws Exception {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(secretKey.getBytes(StandardCharsets.UTF_8));

        if (!signedJWT.verify(verifier)) {
            throw new RuntimeException("JWT 서명 검증에 실패했습니다.");
        }

        return signedJWT.getJWTClaimsSet();
    }
}
