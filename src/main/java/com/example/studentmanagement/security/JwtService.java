package com.example.studentmanagement.security;

import com.example.studentmanagement.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long accessExpirationMillis;

	public JwtService(@Value("${jwt.secret}") String secret,
					  @Value("${jwt.access-expiration-ms}") long accessExpirationMillis) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("jwt.secret must be at least 256 bits");
		}
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		this.accessExpirationMillis = accessExpirationMillis;
	}

	public String generateAccessToken(String username, Collection<Role> roles) {
		Instant issuedAt = Instant.now();
		return Jwts.builder()
				.subject(username)
				.claim("roles", roles.stream().map(Role::name).toList())
				.claim("type", "access")
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(issuedAt.plusMillis(accessExpirationMillis)))
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();
	}

	public Claims parseAccessToken(String token) {
		Claims claims = Jwts.parser().verifyWith(signingKey).build()
				.parseSignedClaims(token).getPayload();
		if (!"access".equals(claims.get("type", String.class)) || claims.getSubject() == null) {
			throw new IllegalArgumentException("Not an access token");
		}
		return claims;
	}

	public long getAccessExpirationSeconds() {
		return accessExpirationMillis / 1000;
	}
}
