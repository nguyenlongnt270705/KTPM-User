package com.example.demo.service;

import com.example.demo.domain.User;
import com.example.demo.dto.ChangePasswordReq;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.excel.ExcelTemplateConfig;
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
import com.example.demo.utils.ExcelBuilder;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
        if (userRepository.existsByUsername(request.getUsername())) {
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
            Role role = roleRepository.findByRoleCode(request.getRoles())
                    .orElseThrow(() -> new ApiInternalException(ErrorMessage.VALIDATION_ERROR));
            user.setRole(role);
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
            Role role = roleRepository.findByRoleCode(request.getRoles())
                    .orElseThrow(() -> new ApiInternalException(ErrorMessage.VALIDATION_ERROR)); // Role not found
            user.setRole(role);
        }

        userRepository.save(user);
        return UserDTO.fromEntity(user);
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

    public void changeActiveUser(Long id, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiInternalException(ErrorMessage.USER_NOT_FOUND));
        user.setActivated(isActive);
        userRepository.save(user);
    }

    public byte[] downloadTemplate() {
        checkAdminPermission();

        // Lấy danh sách role codes từ database
        List<String> roleCodes = roleRepository.findAll().stream()
                .map(Role::getRoleCode)
                .collect(Collectors.toList());

        ExcelTemplateConfig config = ExcelTemplateConfig.builder()
                .headers(List.of("Tên đăng nhập", "Tên", "Email", "Số điện thoại", "Vai trò", "Gói"))
                .fieldRequired(List.of(1, 2, 3)) // Index: 1=Tên đăng nhập, 2=Tên, 3=Email (không tính auto number)
                .autoNumber(true)
                .listValidations(new ArrayList<>())
                .build();

        // Thêm validation cho cột "Vai trò" (index 5 sau khi có auto number)
        config.getListValidations().add(ExcelTemplateConfig.ExcelValidation.builder()
                .rangeName("_ROLE")
                .rowIndex(5) // Index 5 = cột "Vai trò" (sau auto number ở index 0)
                .data(roleCodes)
                .build()
        );

        return ExcelBuilder.buildFileTemplate(config);
    }

    @Transactional(readOnly = true)
    public byte[] exportUser(SearchRequest request) {
        // Kiểm tra quyền admin
        checkAdminPermission();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Users");
            CellStyle headerStyle = ExcelBuilder.createHeaderStyle(wb);

            // Header
            List<String> headers = List.of(
                    "Username",
                    "Full name",
                    "Email",
                    "Phone",
                    "Role",
                    "Role label",
                    "Package",
                    "Activated",
                    "Created date",
                    "Modified date"
            );
            ExcelBuilder.createHeaderRow(sheet, headerStyle, headers);

            // Data: lấy toàn bộ user theo filter (không phân trang để export)
            Specification<User> spec = createSpecification(request);
            List<User> users = userRepository.findAll(spec);

            int rowIndex = 1;
            for (User u : users) {
                Row row = sheet.createRow(rowIndex++);

                // Username
                Cell c0 = row.createCell(0);
                c0.setCellValue(u.getUsername() != null ? u.getUsername() : "");

                // Full name
                Cell c1 = row.createCell(1);
                c1.setCellValue(u.getFullName() != null ? u.getFullName() : "");

                // Email
                Cell c2 = row.createCell(2);
                c2.setCellValue(u.getEmail() != null ? u.getEmail() : "");

                // Phone
                Cell c3 = row.createCell(3);
                c3.setCellValue(u.getPhone() != null ? u.getPhone() : "");

                // Role code
                Cell c4 = row.createCell(4);
                c4.setCellValue(u.getRole() != null && u.getRole().getRoleCode() != null ? u.getRole().getRoleCode() : "");

                // Role description/label
                Cell c5 = row.createCell(5);
                c5.setCellValue(u.getRole() != null && u.getRole().getDescription() != null ? u.getRole().getDescription() : "");

                // Package
                Cell c6 = row.createCell(6);
                c6.setCellValue(u.getUserPackage() != null ? u.getUserPackage() : "");

                // Activated
                Cell c7 = row.createCell(7);
                c7.setCellValue(u.isActivated());

                // Created date (string ISO)
                Cell c8 = row.createCell(8);
                c8.setCellValue(u.getCreatedDate() != null ? u.getCreatedDate().toString() : "");

                // Modified date (string ISO)
                Cell c9 = row.createCell(9);
                c9.setCellValue(u.getModifiedDate() != null ? u.getModifiedDate().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiInternalException(ErrorMessage.UNHANDLED_ERROR, e.getMessage());
        }
    }

    public UserDTO registerUser(String username, String password, String fullName, String email, String phone) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(username)) {
            throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR, "Username already exists");
        }

        // Kiểm tra email đã tồn tại chưa
        if (StringUtils.isNotBlank(email)) {
            boolean emailExists = userRepository.findAll().stream()
                    .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
            if (emailExists) {
                throw new ApiInternalException(ErrorMessage.VALIDATION_ERROR, "Email already exists");
            }
        }

        // Tạo User entity
        User user = User.builder()
                .username(username)
                .login(username) // Set login = username cho đăng ký
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .avatar(User.defaultAvt)
                .activated(true) // Kích hoạt tài khoản ngay sau khi đăng ký
                .build();

        // Mã hóa password
        String encryptedPassword = passwordEncoder.encode(password);
        user.setPassword(encryptedPassword);

        // Set role mặc định là USER
        Role defaultRole = roleRepository.findByRoleCode("USER")
                .orElseThrow(() -> new ApiInternalException(ErrorMessage.VALIDATION_ERROR, "Default USER role not found"));
        user.setRole(defaultRole);

        User savedUser = userRepository.save(user);
        return UserDTO.fromEntity(savedUser);
    }
}

