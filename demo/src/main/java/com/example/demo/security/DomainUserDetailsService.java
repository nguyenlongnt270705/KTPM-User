package com.example.demo.security;

import com.example.demo.domain.User;
import com.example.demo.dto.login.UserProfileDTO;
import com.example.demo.exceptions.CustomAuthenticationException;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component("userDetailsService")
@RequiredArgsConstructor
public class DomainUserDetailsService implements UserDetailsService {

    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(final String login) {
        log.debug("Authenticating {}", login);
        String lowercaseLogin = login.toLowerCase(Locale.ENGLISH);
        return authService.findOneWithAuthoritiesByUsername(lowercaseLogin)
                .map(user -> createSpringSecurityUser(lowercaseLogin, user))
                .orElseThrow(() -> new CustomAuthenticationException("User " + lowercaseLogin + " was not found in the database", HttpStatus.UNAUTHORIZED));
    }

    private CustomUserDetails createSpringSecurityUser(String lowercaseLogin, User user) {
        if (!user.isActivated()) {
            throw new CustomAuthenticationException("User " + lowercaseLogin + " was not activated", HttpStatus.UNAUTHORIZED);
        }
        UserProfileDTO userProfileDTO = UserProfileDTO.fromEntity(user);
        authService.mappingUserPermissions(userProfileDTO, user);
        List<GrantedAuthority> grantedAuthorities = userProfileDTO.getPermissions().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
        return new CustomUserDetails(
                user.getUsername(),
                user.getPassword(),
                grantedAuthorities,
                userProfileDTO.getRoles()
        );
    }

}
