package com.example.demo.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role {
    @Id
    private String roleCode;

    @NotNull(message = "Role name is required")
    private String roleName;
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_code"),
        inverseJoinColumns = @JoinColumn(name = "permission_code")
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
