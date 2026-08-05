package com.marketplace.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;
	private JwtProperties jwtProperties;

	@BeforeEach
	void setUp() {
		jwtProperties = new JwtProperties();
		jwtProperties.setAccessSecret("test-access-secret-for-unit-tests-must-be-long-enough");
		jwtProperties.setRefreshSecret("test-refresh-secret-for-unit-tests-must-be-long-enough");
		jwtProperties.setAccessTokenExpiration(900_000); // 15 min
		jwtProperties.setRefreshTokenExpiration(604_800_000); // 7 days
		jwtTokenProvider = new JwtTokenProvider(jwtProperties);
	}

	// ==================== GENERATE ACCESS TOKEN ====================

	@Test
	void generateAccessToken_containsCorrectClaims() {
		String token = jwtTokenProvider.generateAccessToken("user-id-123", "test@example.com", "BUYER");

		Claims claims = parseToken(token, jwtProperties.getAccessSecret());

		assertThat(claims.getSubject()).isEqualTo("user-id-123");
		assertThat(claims.get("email")).isEqualTo("test@example.com");
		assertThat(claims.get("role")).isEqualTo("BUYER");
		assertThat(claims.getIssuedAt()).isNotNull();
		assertThat(claims.getExpiration()).isAfter(new Date());
	}

	@Test
	void generateAccessToken_withSellerRole() {
		String token = jwtTokenProvider.generateAccessToken("seller-id", "seller@example.com", "SELLER");

		Claims claims = parseToken(token, jwtProperties.getAccessSecret());

		assertThat(claims.get("role")).isEqualTo("SELLER");
	}

	@Test
	void generateAccessToken_withAdminRole() {
		String token = jwtTokenProvider.generateAccessToken("admin-id", "admin@example.com", "ADMIN");

		Claims claims = parseToken(token, jwtProperties.getAccessSecret());

		assertThat(claims.get("role")).isEqualTo("ADMIN");
	}

	// ==================== GENERATE REFRESH TOKEN ====================

	@Test
	void generateRefreshToken_hasSubjectAndJti() {
		String token = jwtTokenProvider.generateRefreshToken("user-id-123");

		Claims claims = parseToken(token, jwtProperties.getRefreshSecret());

		assertThat(claims.getSubject()).isEqualTo("user-id-123");
		assertThat(claims.getId()).isNotBlank();
	}

	@Test
	void generateRefreshToken_eachTokenHasUniqueJti() {
		String token1 = jwtTokenProvider.generateRefreshToken("user-id-123");
		String token2 = jwtTokenProvider.generateRefreshToken("user-id-123");

		Claims claims1 = parseToken(token1, jwtProperties.getRefreshSecret());
		Claims claims2 = parseToken(token2, jwtProperties.getRefreshSecret());

		assertThat(claims1.getId()).isNotEqualTo(claims2.getId());
	}

	// ==================== GENERATE MFA TOKEN ====================

	@Test
	void generateMfaToken_hasMfaTypeClaim() {
		String token = jwtTokenProvider.generateMfaToken("user-id-123");

		// MFA token is signed with refreshKey + "-mfa"
		String mfaSecret = jwtProperties.getRefreshSecret() + "-mfa";
		Claims claims = parseToken(token, mfaSecret);

		assertThat(claims.getSubject()).isEqualTo("user-id-123");
		assertThat(claims.get("type")).isEqualTo("mfa");
	}

	// ==================== VALIDATE TOKEN ====================

	@Test
	void validateToken_validAccess_returnsTrue() {
		String token = jwtTokenProvider.generateAccessToken("user-id", "test@example.com", "BUYER");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.ACCESS);

		assertThat(valid).isTrue();
	}

	@Test
	void validateToken_validRefresh_returnsTrue() {
		String token = jwtTokenProvider.generateRefreshToken("user-id");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.REFRESH);

		assertThat(valid).isTrue();
	}

	@Test
	void validateToken_validMfa_returnsTrue() {
		String token = jwtTokenProvider.generateMfaToken("user-id");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.MFA);

		assertThat(valid).isTrue();
	}

	@Test
	void validateToken_expiredAccess_returnsFalse() {
		JwtProperties expiredProps = new JwtProperties();
		expiredProps.setAccessSecret("test-access-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setRefreshSecret("test-refresh-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setAccessTokenExpiration(-1); // already expired
		expiredProps.setRefreshTokenExpiration(604_800_000);
		JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

		String token = expiredProvider.generateAccessToken("user-id", "test@example.com", "BUYER");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.ACCESS);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_expiredRefresh_returnsFalse() {
		JwtProperties expiredProps = new JwtProperties();
		expiredProps.setAccessSecret("test-access-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setRefreshSecret("test-refresh-secret-for-unit-tests-must-be-long-enough");
		expiredProps.setAccessTokenExpiration(900_000);
		expiredProps.setRefreshTokenExpiration(-1); // already expired
		JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);

		String token = expiredProvider.generateRefreshToken("user-id");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.REFRESH);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_wrongKey_returnsFalse() {
		// Generate with access key, validate with refresh key
		String token = jwtTokenProvider.generateAccessToken("user-id", "test@example.com", "BUYER");

		boolean valid = jwtTokenProvider.validateToken(token, TokenType.REFRESH);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_malformedToken_returnsFalse() {
		boolean valid = jwtTokenProvider.validateToken("not-a-valid-jwt", TokenType.ACCESS);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_emptyString_returnsFalse() {
		boolean valid = jwtTokenProvider.validateToken("", TokenType.ACCESS);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_tamperedPayload_returnsFalse() {
		String token = jwtTokenProvider.generateAccessToken("user-id", "test@example.com", "BUYER");
		// Tamper with the token by changing a character in the payload
		String[] parts = token.split("\\.");
		parts[1] = parts[1].substring(0, parts[1].length() - 2) + "XX";
		String tampered = String.join(".", parts);

		boolean valid = jwtTokenProvider.validateToken(tampered, TokenType.ACCESS);

		assertThat(valid).isFalse();
	}

	@Test
	void validateToken_nullToken_returnsFalse() {
		boolean valid = jwtTokenProvider.validateToken(null, TokenType.ACCESS);

		assertThat(valid).isFalse();
	}

	// ==================== GET USER ID FROM TOKEN ====================

	@Test
	void getUserIdFromToken_returnsSubject() {
		String token = jwtTokenProvider.generateAccessToken("user-id-123", "test@example.com", "BUYER");

		String userId = jwtTokenProvider.getUserIdFromToken(token, TokenType.ACCESS);

		assertThat(userId).isEqualTo("user-id-123");
	}

	@Test
	void getUserIdFromToken_refreshToken_returnsSubject() {
		String token = jwtTokenProvider.generateRefreshToken("user-id-456");

		String userId = jwtTokenProvider.getUserIdFromToken(token, TokenType.REFRESH);

		assertThat(userId).isEqualTo("user-id-456");
	}

	// ==================== HELPER ====================

	private Claims parseToken(String token, String secret) {
		// JwtTokenProvider does: Keys.hmacShaKeyFor(Decoders.BASE64.decode(Base64.encode(secret.getBytes())))
		// which is a roundtrip — net effect is just secret.getBytes()
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}
}
