package com.example.taskai.auth.service;

import com.example.taskai.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	private final String secret;

	private final long expirationMillis;

	public TokenService(
		@Value("${jwt.secret:dev-secret-key-change-in-production}") String secret,
		@Value("${jwt.expiration:86400000}") long expirationMillis
	) {
		this.secret = secret;
		this.expirationMillis = expirationMillis;
	}

	public String createToken(String username, String role) {
		long expiresAt = Instant.now().toEpochMilli() + expirationMillis;
		String payload = username + "|" + role + "|" + expiresAt;
		String signature = sign(payload);
		return Base64.getUrlEncoder()
			.withoutPadding()
			.encodeToString((payload + "|" + signature).getBytes(StandardCharsets.UTF_8));
	}

	public TokenPayload parseToken(String token) {
		if (token == null || token.isBlank()) {
			throw unauthorized();
		}
		try {
			String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
			String[] parts = decoded.split("\\|");
			if (parts.length != 4) {
				throw unauthorized();
			}

			String payload = parts[0] + "|" + parts[1] + "|" + parts[2];
			if (!sign(payload).equals(parts[3])) {
				throw unauthorized();
			}

			long expiresAt = Long.parseLong(parts[2]);
			if (expiresAt < Instant.now().toEpochMilli()) {
				throw unauthorized();
			}

			return new TokenPayload(parts[0], parts[1]);
		}
		catch (BusinessException e) {
			throw e;
		}
		catch (Exception e) {
			throw unauthorized();
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException("Token 签名失败", e);
		}
	}

	private BusinessException unauthorized() {
		return new BusinessException(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED);
	}

	public record TokenPayload(String username, String role) {
	}
}
