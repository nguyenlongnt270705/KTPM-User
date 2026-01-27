package com.example.demo.security.jwt;

import com.example.demo.config.ApplicationProperties;
import com.example.demo.dto.NotificationPayload;
import com.example.demo.enums.NotificationEnum;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.service.RedisTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final String ROLES_KEY = "role";
    private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";
    private final Key key;
    private final JwtParser jwtParser;
    private final long tokenValidityInMilliseconds;
    private final long tokenValidityInMillisecondsForRememberMe;
    private final long refreshTokenValidityInMilliseconds;
    private final SimpMessagingTemplate messagingTemplate;
    @Getter
    private final RedisTokenService redisTokenService;

    public TokenProvider(ApplicationProperties properties, SimpMessagingTemplate messagingTemplate,
                         RedisTokenService redisTokenService) {
        this.messagingTemplate = messagingTemplate;
        this.redisTokenService = redisTokenService;
        String secret = properties.getSecurity().getBase64Secret();
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        key = Keys.hmacShaKeyFor(keyBytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
        this.tokenValidityInMilliseconds = 1000 * properties.getSecurity().getTokenValidityInSeconds();
        this.tokenValidityInMillisecondsForRememberMe = 1000 * properties.getSecurity().getTokenValidityInSeconds();
        this.refreshTokenValidityInMilliseconds = 1000 * (properties.getSecurity().getRefreshTokenValidityInSeconds() != null
                ? properties.getSecurity().getRefreshTokenValidityInSeconds() : 86400L * 7);
    }

    public String createToken(Authentication authentication, boolean rememberMe, HttpServletRequest request) {
        String authorities =
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        String jwtName = authentication.getName();
        String tokenExisting = redisTokenService.getToken(jwtName);
        HttpSession session = request.getSession(true);
        String sessionId = session.getId();

        if (tokenExisting != null) {
            NotificationPayload<?> payload = NotificationPayload.builder()
                    .type(NotificationEnum.FORCE_LOGOUT)
                    .message("Tài khoản của bạn vừa đăng nhập ở nơi khác")
                    .title("Phiên đăng nhập")
                    .sessionId(sessionId)
                    .timestamp(Instant.now())
                    .username(userDetails.getUsername())
                    .build();
            messagingTemplate.convertAndSendToUser(userDetails.getUsername(), "/queue/force-logout", payload);
            this.revokeToken(tokenExisting);
        }

        String token = Jwts.builder()
                .setId(sessionId)
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(ROLES_KEY, userDetails.getRoleCode())
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();

        redisTokenService.saveToken(jwtName, token, tokenValidityInMilliseconds / (1000 * 60));
        return token;
    }

    public String createRefreshToken(String username, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String sessionId = session.getId();
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValidityInMilliseconds);

        String refreshToken = Jwts.builder()
                .setId(sessionId + "_refresh")
                .setSubject(username)
                .claim("type", "refresh")
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();

        // Lưu refresh token vào Redis với thời gian hết hạn dài hơn
        redisTokenService.saveRefreshToken(username, refreshToken, refreshTokenValidityInMilliseconds / (1000 * 60));
        return refreshToken;
    }

    public Claims validateRefreshToken(String refreshToken) {
        try {
            Claims claims = jwtParser.parseClaimsJws(refreshToken).getBody();
            String tokenType = claims.get("type", String.class);

            if (!"refresh".equals(tokenType)) {
                log.error("Invalid refresh token type");
                return null;
            }

            String username = claims.getSubject();

            // Kiểm tra refresh token có hợp lệ trong Redis không
            if (!redisTokenService.isRefreshTokenValid(username, refreshToken)) {
                log.error("Refresh token not found in Redis or expired");
                return null;
            }

            return claims;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.trace("Invalid refresh token", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Refresh token validation error {}", e.getMessage());
            return null;
        }
    }

    public String createAccessTokenFromAuthentication(Authentication authentication, HttpServletRequest request) {
        return createToken(authentication, false, request);
    }

    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        Collection<? extends GrantedAuthority> authorities = Arrays
                .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                .filter(auth -> !auth.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        CustomUserDetails principal = new CustomUserDetails(
                claims.getSubject(),
                "",
                authorities,
                claims.get(ROLES_KEY, String.class)
        );
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            Claims claims = jwtParser.parseClaimsJws(authToken).getBody();
            String username = claims.getSubject(); // lấy JWT ID

            //kiểm tra token có hợp lệ không
            return redisTokenService.isTokenValid(username, authToken);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            log.trace(INVALID_JWT_TOKEN, e);
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }
        return false;
    }

    public void revokeToken(String token) {
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        redisTokenService.deleteToken(claims.getId());
    }

}
