package com.example.taskai.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.user.dto.UserCreateRequest;
import com.example.taskai.user.dto.UserPasswordRequest;
import com.example.taskai.user.dto.UserStatusRequest;
import com.example.taskai.user.dto.UserUpdateRequest;
import com.example.taskai.user.service.UserService;
import com.example.taskai.user.vo.UserDetailVO;
import com.example.taskai.user.vo.UserListItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<IPage<UserListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(userService.listUsers(keyword, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(userService.getUserDetail(id));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserCreateRequest request) {
        userService.createUser(request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody UserUpdateRequest request) {
        userService.updateUser(id, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody UserStatusRequest request) {
        userService.updateUserStatus(id, request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody UserPasswordRequest request) {
        userService.resetPassword(id, request);
        return ApiResponse.ok(null);
    }
}
