package com.example.demo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@Entity
@Table(name = "permissions")
public class Permission extends AbstractAuditingEntity {

    @Id
    private String permissionCode;
    private String type;
    private String description;
    private String method;
    private String module;
    private String pathPattern;
}
