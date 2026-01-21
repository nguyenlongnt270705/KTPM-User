package com.example.demo.dto.login;

import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private boolean activated;
    private String userPackage;
    private String roles;
    private String roleLabels;
    private List<String> permissions;

    public static UserProfileDTO fromEntity(User user) {
        return UserProfileDTO.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .activated(user.isActivated())
                .userPackage(user.getUserPackage())
                .roles(user.getRole() != null ? user.getRole().getRoleCode() : null)
                .roleLabels(user.getRole() != null ? user.getRole().getDescription() : null)
                .build();
    }
}
