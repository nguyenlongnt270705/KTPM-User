package com.example.demo.repository;

import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<User> findOneWithAuthoritiesByUsername(String username);

    Boolean existUserByUsername(String username);
}

