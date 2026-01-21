package com.example.demo.service;

import com.example.demo.domain.Permission;
import com.example.demo.domain.User;
import com.example.demo.domain.UserPermissions;
import com.example.demo.dto.login.UserProfileDTO;
import com.example.demo.enums.PermissionAction;
import com.example.demo.exceptions.ApiInternalException;
import com.example.demo.exceptions.CustomAuthenticationException;
import com.example.demo.exceptions.ErrorMessage;
import com.example.demo.repository.UserPermissionsRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserPermissionsRepository userPermissionsRepository;

    public Optional<User> findOneWithAuthoritiesByUsername(String username) {
        return userRepository.findOneWithAuthoritiesByUsername(username);
    }

    public UserProfileDTO getProfile() {
        String username = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new CustomAuthenticationException("User not authenticated", HttpStatus.UNAUTHORIZED));
        Optional<User> user = userRepository.findOneWithAuthoritiesByUsername(username);
        if (user.isEmpty()) {
            throw new ApiInternalException(ErrorMessage.USER_NOT_FOUND);
        }
        UserProfileDTO userProfileDTO = UserProfileDTO.fromEntity(user.get());
        this.mappingUserPermissions(userProfileDTO, user.get());
        return userProfileDTO;
    }

    public void mappingUserPermissions(UserProfileDTO userProfileDTO, User user) {
        Set<String> permissions = new HashSet<>();
        if (user.getRole() != null && !ObjectUtils.isEmpty(user.getRole().getPermissions())) {
            permissions = user.getRole().getPermissions().stream()
                    .filter(Objects::nonNull)
                    .map(Permission::getPermissionCode)
                    .collect(Collectors.toSet());
        }
        List<UserPermissions> userPermissions = userPermissionsRepository.findAllByUserId(user.getUserId());
        if (!userPermissions.isEmpty()) {
            permissions.addAll(userPermissions.stream()
                    .filter(pm -> PermissionAction.GRANT.equals(pm.getAction()))
                    .map(UserPermissions::getPermissionCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
            permissions.removeAll(userPermissions.stream()
                    .filter(pm -> PermissionAction.DENY.equals(pm.getAction()))
                    .map(UserPermissions::getPermissionCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }
        userProfileDTO.setPermissions(new ArrayList<>(permissions));
    }
}
