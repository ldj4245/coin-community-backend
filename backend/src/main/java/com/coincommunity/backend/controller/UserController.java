package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.UserDto;
import com.coincommunity.backend.service.UserService;
import com.coincommunity.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 사용자 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 등록
     */
    @PostMapping("/register")
    @Operation(summary = "사용자 등록", description = "새로운 사용자를 등록합니다.")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        
        UserDto.UserResponse response = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "사용자 로그인", description = "사용자 로그인을 처리합니다.")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> login(
            @Valid @RequestBody UserDto.LoginRequest request) {
        
        UserDto.LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 정보 조회
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 정보 조회", description = "특정 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> getUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId) {
        
        UserDto.UserResponse response = userService.findById(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> getMyInfo() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserDto.UserResponse response = userService.findById(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 업데이트
     */
    @PutMapping("/me/profile")
    @Operation(summary = "프로필 업데이트", description = "현재 로그인한 사용자의 프로필을 업데이트합니다.")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> updateProfile(
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserDto.UserResponse response = userService.updateProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자명으로 조회
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "사용자명으로 조회", description = "사용자명으로 사용자 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserDto.UserResponse>> getUserByUsername(
            @Parameter(description = "사용자명") @PathVariable String username) {
        
        UserDto.UserResponse response = userService.findByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}