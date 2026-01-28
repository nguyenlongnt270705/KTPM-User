package com.example.demo.controller;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.search.SearchRequest;
import com.example.demo.dto.search.SearchResponse;
import com.example.demo.exceptions.ResponseObject;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class UserController {

    private final UserService userService;


    @PostMapping("/users/search-datatable")
    public ResponseEntity<ResponseObject<SearchResponse<UserDTO>>> search(@ModelAttribute SearchRequest criteria) {
        SearchResponse<UserDTO> result = userService.searchDatatable(criteria);
        return ResponseEntity.ok(ResponseObject.success(result));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ResponseObject<UserDTO>> getUserById(@PathVariable Long id) {
        UserDTO result = userService.findById(id);
        return ResponseEntity.ok(ResponseObject.success(result));
    }

    @PostMapping("/users/create")
    public ResponseEntity<ResponseObject<UserDTO>> createUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO newUserDTO = userService.createUser(userDTO);
        return ResponseEntity.ok(ResponseObject.success(newUserDTO));
    }

    @PutMapping("/users/update")
    public ResponseEntity<ResponseObject<UserDTO>> updateUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(userDTO);
        return ResponseEntity.ok(ResponseObject.success(updatedUser));
    }

    @GetMapping("/users/deactive/{id}")
    public ResponseEntity<ResponseObject<Boolean>> deactivateUser(@PathVariable Long id) {
        userService.changeActiveUser(id, false);
        return ResponseEntity.ok(ResponseObject.success());
    }

    @GetMapping("/users/active/{id}")
    public ResponseEntity<ResponseObject<Boolean>> activeUser(@PathVariable Long id) {
        userService.changeActiveUser(id, true);
        return ResponseEntity.ok(ResponseObject.success());
    }

    @GetMapping("/users/file-template")
    public ResponseEntity<byte[]> downloadFileTemplate(@RequestParam("userId") String userId) {
        String filename = userId.toLowerCase() + "_user-import-template.xlsx";
        byte[] file = userService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/lovephim.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping("/users/export")
    public ResponseEntity<byte[]> exportUser(@RequestBody SearchRequest request) {
        String fileName = "export-user-" + LocalDate.now() + ".xlsx";
        byte[] file = userService.exportUser(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/lovephim.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
