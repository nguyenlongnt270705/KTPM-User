package com.example.demo.controller;

import com.example.demo.domain.User;
import com.example.demo.dto.UserDTO;
import com.example.demo.dto.search.SearchRequest;
import com.example.demo.dto.search.SearchResponse;
import com.example.demo.exceptions.ResponseObject;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        this.userService.deleteUser(id);
        return ResponseEntity.ok(null);
    }
}
