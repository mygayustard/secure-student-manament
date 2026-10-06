package com.example.studentmanagement.auth;

import com.example.studentmanagement.user.User;
import com.example.studentmanagement.user.Role;
import com.example.studentmanagement.exception.InvalidTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final Duration lifetime;
	private final SecureRandom secureRandom = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
	                           @Value("${jwt.refresh-expiration-ms}") long lifetimeMillis) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.lifetime = Duration.ofMillis(lifetimeMillis);
	}

	@Transactional
	public String issue(User user) {
		if (lifetime.isZero() || lifetime.isNegative()) {
			throw new IllegalArgumentException("Token lifetime must be positive");
		}

		String rawToken = createRawToken();
		refreshTokenRepository.save(createEntity(user, rawToken));
		return rawToken;
	}

	@Transactional
	public TokenRotation rotate(String rawToken) {
		RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
				.filter(token -> !token.isRevoked() && token.getExpiresAt().isAfter(Instant.now()))
				.orElseThrow(() -> new InvalidTokenException("Refresh token is invalid or expired"));
		current.setRevoked(true);
		String replacement = createRawToken();
		refreshTokenRepository.save(createEntity(current.getUser(), replacement));
		User user = current.getUser();
		return new TokenRotation(user.getUsername(), Set.copyOf(user.getRoles()), replacement);
	}

	@Transactional
	public void revoke(String rawToken) {
		refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
				.filter(token -> !token.isRevoked())
				.ifPresent(token -> {
					token.setRevoked(true);
					refreshTokenRepository.save(token);
				});
	}

	@Transactional
	public void deleteExpiredTokens() {
		refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
	}

	private String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private RefreshToken createEntity(User user, String rawToken) {
		RefreshToken token = new RefreshToken();
		token.setTokenHash(hash(rawToken));
		token.setUser(user);
		token.setExpiresAt(Instant.now().plus(lifetime));
		return token;
	}

	private String createRawToken() {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
	}

	public record TokenRotation(String username, Set<Role> roles, String refreshToken) {
	}
}
