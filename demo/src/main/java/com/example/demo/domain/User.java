package com.example.demo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User extends AbstractAuditingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotEmpty(message = "Username is required")
    private String username;

    @NotEmpty(message = "Missing password")
    @Size(min = 3, message = "Password must be 3 characters or more")
    private String password;

    @Email(message = "Email is invalid")
    private String email;

    @NotEmpty(message = "Full name is required")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone is required")
    private String phone;

    public static final String defaultAvt = "https://img.freepik.com/free-icon/user_318-159711.jpg";
    @Builder.Default
    private String avatar = defaultAvt;

    @Builder.Default
    private boolean activated = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_code")
    private Role role;

    private String userPackage;
    private String login;
}
