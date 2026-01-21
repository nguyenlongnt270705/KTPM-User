package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.ChangePasswordReq;
import com.example.demo.dto.login.JWTToken;
import com.example.demo.dto.login.LoginVM;
import com.example.demo.dto.login.UserProfileDTO;
import com.example.demo.exceptions.ResponseObject;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.security.jwt.JWTFilter;
import com.example.demo.security.jwt.TokenProvider;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserJWTController {

    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final AuthService authService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM, HttpServletRequest request) {
        // Xác thực username và password
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginVM.getUsername(),
                loginVM.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Tạo JWT token và refresh token
        String jwt = tokenProvider.createToken(authentication, loginVM.isRememberMe(), request);
        String refreshToken = tokenProvider.createRefreshToken(authentication.getName(), request);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        return new ResponseEntity<>(new JWTToken(jwt, refreshToken), httpHeaders, HttpStatus.OK);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JWTToken> refreshToken(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        String refreshToken = requestBody.get("refreshToken");

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Validate refresh token và lấy claims
        Claims claims = tokenProvider.validateRefreshToken(refreshToken);
        if (claims == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = claims.getSubject();

        // Lấy thông tin User và tạo lại authentication
        try {
            Optional<User> userOpt = authService.findOneWithAuthoritiesByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userOpt.get();
            UserProfileDTO userProfileDTO = UserProfileDTO.fromEntity(user);
            authService.mappingUserPermissions(userProfileDTO, user);

            // Tạo lại authentication từ user profile
            Collection<GrantedAuthority> authorities = userProfileDTO.getPermissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            CustomUserDetails userDetails = new CustomUserDetails(
                    username,
                    "",
                    authorities,
                    String.join(",", userProfileDTO.getRoles())

            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

            // Tạo access token mới
            String newAccessToken = tokenProvider.createAccessTokenFromAuthentication(authentication, request);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add(JWTFilter.AUTHORIZATION_HEADER, newAccessToken);

            return new ResponseEntity<>(new JWTToken(newAccessToken, refreshToken), httpHeaders, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user-profile")
    public ResponseEntity<ResponseObject<UserProfileDTO>> getUserProfile() {
        UserProfileDTO userProfile = authService.getProfile();
        return ResponseEntity.ok(ResponseObject.success(userProfile));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ResponseObject<Boolean>> changePassword(@RequestBody ChangePasswordReq req) {
        userService.changePassword(req);
        return ResponseEntity.ok(ResponseObject.success(true));
    }

    @PostMapping("/update-user-profile")
    public ResponseEntity<ResponseObject<Boolean>> updateUserProfile(@RequestBody UserProfileDTO req) {
        userService.updateUserProfile(req);
        return ResponseEntity.ok(ResponseObject.success(true));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            @RequestBody(required = false) Map<String, String> requestBody
    ) {
        if (bearerToken != null || bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            tokenProvider.revokeToken(token); // Xoá access token trong Redis
        }

        // Xoá refresh token nếu có
        if (requestBody != null && requestBody.containsKey("refreshToken")) {
            String refreshToken = requestBody.get("refreshToken");
            try {
                Claims claims = tokenProvider.validateRefreshToken(refreshToken);
                if (claims != null) {
                    String username = claims.getSubject();
                    tokenProvider.getRedisTokenService().deleteRefreshToken(username);
                }
            } catch (Exception e) {
                log.warn("Error deleting refresh token on logout", e);
            }
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
