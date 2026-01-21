package com.example.demo.repository;

import com.example.demo.domain.UserPermissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface UserPermissionsRepository extends JpaRepository<UserPermissions, Long> {
    List<UserPermissions> findAllByUserId(Long userId);

    @Modifying
    void deleteAllByUserId(Long userId);
}
