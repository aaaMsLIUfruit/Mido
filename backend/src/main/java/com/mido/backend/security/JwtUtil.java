package com.mido.backend.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.mido.backend.user.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final JwtProperties properties;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES);
        return JWT.create()
                .withSubject(user.getUsername())
                .withClaim("uid", user.getId())
                .withClaim("email", user.getEmail())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiration))
                .sign(algorithm);
    }

    public String extractUsername(String token) {
        DecodedJWT decodedJWT = decode(token);
        return decodedJWT != null ? decodedJWT.getSubject() : null;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username != null && username.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        DecodedJWT decodedJWT = decode(token);
        return decodedJWT == null || decodedJWT.getExpiresAt().before(new Date());
    }

    private DecodedJWT decode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException ex) {
            return null;
        }
    }
}

