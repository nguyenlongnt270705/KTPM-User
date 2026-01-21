package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.dto.ChangePasswordReq;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.login.UserProfileDTO;
import com.example.demo.dto.search.SearchRequest;
import com.example.demo.dto.search.SearchResponse;
import com.example.demo.exceptions.ApiInternalException;
import com.example.demo.exceptions.CustomAuthenticationException;
import com.example.demo.exceptions.ErrorMessage;
import com.example.demo.domain.Role;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.SecurityUtils;
import com.example.demo.utils.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final RoleRepository roleRepository;


    @Transactional(readOnly = true)
    public SearchResponse<UserDTO> searchDatatable(SearchRequest request) {
        // Kiểm tra quyền admin
        checkAdminPermission();

        Specification<User> spec = createSpecification(request);
        Page<User> users = userRepository.findAll(spec, request.toPageable());
        return new SearchResponse<>(
                users.getContent().stream().map(UserDTO::fromEntity).collect(Collectors.toList()),
                users.getTotalElements()
        );
    }

    private void checkAdminPermission() {
        String username = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new CustomAuthenticationException("User not authenticated", HttpStatus.UNAUTHORIZED));

        Optional<User> currentUser = authService.findOneWithAuthoritiesByUsername(username);
        if (currentUser.isEmpty()) {
            throw new ApiInternalException(ErrorMessage.USER_NOT_FOUND);
        }

        User user = currentUser.get();
        // Kiểm tra nếu role là ADMIN
        if (user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getRoleCode())) {
            throw new CustomAuthenticationException("Access denied. Only admin can access this function.", HttpStatus.FORBIDDEN);
        }
    }

    private Specification<User> createSpecification(SearchRequest criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(criteria.searchText())) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("username")), "%" + criteria.searchText().toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("fullName")), "%" + criteria.searchText().toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("email")), "%" + criteria.searchText().toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("phone")), "%" + criteria.searchText().toLowerCase() + "%")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public UserDTO findById(Long id) {
        return userRepository.findById(id).map(UserDTO::new).
                orElseThrow(() -> new ApiInternalException(ErrorMessage.USER_NOT_FOUND));
    }

    public UserDTO createUser(UserDTO request) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.existUserByUsername(request.getUsername())) {
            throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR);
        }

        // Tạo User entity từ UserDTO
        User user = User.builder()
                .username(request.getUsername())
                .login(request.getLogin())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .avatar(request.getAvatar() != null ? request.getAvatar() : User.defaultAvt)
                .activated(request.isActivated())
                .userPackage(request.getUserPackage())
                .build();

        // Mã hóa password từ login field (login là password ban đầu mà admin set)
        String encryptedPassword = passwordEncoder.encode(user.getLogin());
        user.setPassword(encryptedPassword);
        
        // Set role nếu có
        if (ObjectUtils.isNotEmpty(request.getRoles())) {
            Role role = roleRepository.findAllByCode(request.getRoles());
            if (role != null) {
                user.setRole(role);
            } else {
                throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR);
            }
        }
        
        User savedUser = userRepository.save(user);
        return UserDTO.fromEntity(savedUser);
    }

    public UserDTO updateUser(UserDTO request) {
        // Kiểm tra quyền admin
        checkAdminPermission();

        if (request.getUserId() == null) {
            throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ApiInternalException(ErrorMessage.USER_NOT_FOUND));

        // Update
        if (StringUtils.isNotBlank(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.isNotBlank(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.isNotBlank(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        user.setActivated(request.isActivated());
        if (StringUtils.isNotBlank(request.getUserPackage())) {
            user.setUserPackage(request.getUserPackage());
        }

        // Update role nếu có
        if (ObjectUtils.isNotEmpty(request.getRoles())) {
            Role role = roleRepository.findAllByCode(request.getRoles());
            if (role != null) {
                user.setRole(role);
            } else {
                throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR); // Role not found
            }
        }

        userRepository.save(user);
        return UserDTO.fromEntity(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    public void changePassword(ChangePasswordReq req) {
        if (CommonUtils.isEmpty(req.getCurrentPassword(), req.getNewPassword())) {
            throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR);
        }

        String userLogin = SecurityUtils.getCurrentUserLogin().get();
        Optional<User> optionalUser = userRepository.findByUsername(userLogin);
        if (optionalUser.isEmpty()) {
            throw new ApiInternalException(ErrorMessage.USER_NOT_FOUND);
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new ApiInternalException(ErrorMessage.CURRENT_PASSWORD_INVALID);
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    public void updateUserProfile(UserProfileDTO request) {
        String userLogin = SecurityUtils.getCurrentUserLogin().get();
        Optional<User> optionalUser = userRepository.findByUsername(userLogin);
        if (optionalUser.isEmpty()) {
            throw new ApiInternalException(ErrorMessage.USER_NOT_FOUND);
        }
        User user = optionalUser.get();
        if (StringUtils.isNotBlank(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (StringUtils.isNotBlank(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.isNotBlank(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        userRepository.save(user);
    }
}

