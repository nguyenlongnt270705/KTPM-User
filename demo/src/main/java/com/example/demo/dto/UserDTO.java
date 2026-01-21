package com.example.demo.dto;

import com.example.demo.domain.User;
import com.example.demo.utils.json.InstantToStringSerializer;
import com.example.demo.utils.json.LowerCaseTrimDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    @JsonDeserialize(using = LowerCaseTrimDeserializer.class)
    private String username;
    @JsonDeserialize(using = LowerCaseTrimDeserializer.class)
    private String login;

    private String fullName;
    private String email;
    private String phone;
    private String avatar;
    private boolean activated;
    private String userPackage;
    private String roles;
    private String roleLabels;

    // ngày tạo tk
    @JsonSerialize(using = InstantToStringSerializer.class)
    private Instant createdDate;

    // ngày hết hạn package
    @JsonSerialize(using = InstantToStringSerializer.class)
    private Instant modifiedDate;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .login(user.getLogin())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .activated(user.isActivated())
                .userPackage(user.getUserPackage())
                .roles(user.getRole() != null ? user.getRole().getRoleCode() : null)
                .roleLabels(user.getRole() != null ? user.getRole().getDescription() : null)
                .createdDate(user.getCreatedDate())
                .modifiedDate(user.getModifiedDate())
                .build();
    }

    public UserDTO(User user) {
        this.userId = user.getUserId();
        this.login = user.getLogin();
        this.fullName = user.getFullName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.avatar = user.getAvatar();
        this.activated = user.isActivated();
        this.userPackage = user.getUserPackage();
        this.roles = user.getRole() != null ? user.getRole().getRoleCode() : null;
        this.roleLabels = user.getRole() != null ? user.getRole().getDescription() : null;
        this.createdDate = user.getCreatedDate();
        this.modifiedDate = user.getModifiedDate();
    }
}
