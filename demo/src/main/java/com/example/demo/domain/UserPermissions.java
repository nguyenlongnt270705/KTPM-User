package com.example.demo.domain;

import com.example.demo.enums.PermissionAction;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_permissions")
public class UserPermissions extends AbstractAuditingEntity {

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private Long userId;

    @Column(name = "permission_code", insertable = false, updatable = false)
    private String permissionCode;

    @Enumerated(EnumType.STRING)
    private PermissionAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_code", referencedColumnName = "permissionCode")
    private Permission permission;
}
