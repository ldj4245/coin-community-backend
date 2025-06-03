package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.UserDto;
import com.coincommunity.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API 엔드포인트
 * 기본 경로: /api/users
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리", description = "회원가입, 로그인, 프로필 관리 등 사용자 관련 API")
public class UserController {

    private final UserService userService;

    /**
     * 회원가입
     */
    @Operation(
        summary = "회원가입",
        description = "새로운 사용자 계정을 생성합니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses(value = {
    })
    @PostMapping("/register")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<UserDto.ProfileResponse>> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        UserDto.ProfileResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 로그인
     */
    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인합니다. 성공 시 JWT 토큰이 반환됩니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses(value = {
    })
    @PostMapping("/login")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<UserDto.LoginResponse>> login(
            @Valid @RequestBody UserDto.LoginRequest request) {
        UserDto.LoginResponse response = userService.login(request);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @Operation(
        summary = "현재 사용자 정보 조회",
        description = "현재 로그인한 사용자의 상세 정보를 조회합니다. 인증이 필요합니다.",
        tags = {"사용자 관리"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/me")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<UserDto.ProfileResponse>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UserDto.ProfileResponse response = userService.getUserProfile(
                Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 특정 사용자 정보 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> getUserById(
            @PathVariable Long id) {
        UserDto.ProfileResponse response = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 업데이트
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.ProfileResponse response = userService.updateProfile(
                Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.PasswordChangeRequest request) {
        userService.changePassword(
                Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 특정 사용자의 글 목록 조회
     */
    @GetMapping("/{id}/posts")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.PostSummaryResponse>>> getUserPosts(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<UserDto.PostSummaryResponse> page = userService.getUserPosts(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * 특정 사용자의 댓글 목록 조회
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<PageResponse<UserDto.CommentSummaryResponse>>> getUserComments(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<UserDto.CommentSummaryResponse> page = userService.getUserComments(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * 포인트 충전 (테스트용)
     */
    @PostMapping("/me/points")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> addPoints(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDto.PointRequest request) {
        UserDto.ProfileResponse response = userService.addPoints(
                Long.parseLong(userDetails.getUsername()), request.getPoints());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
