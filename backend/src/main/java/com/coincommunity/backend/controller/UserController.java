package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.UserDto;
import com.coincommunity.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 존재하는 이메일"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> register(
            @Valid @RequestBody UserDto.RegisterRequest request) {
        UserDto.ProfileResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 로그인
     */
    @Operation(
        summary = "로그인",
        description = "이메일과 비밀번호로 로그인합니다. 성공 시 JWT 토큰이 반환됩니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto.LoginResponse>> login(
            @Valid @RequestBody UserDto.LoginRequest request) {
        UserDto.LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> getCurrentUser(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        UserDto.ProfileResponse response = userService.getUserProfile(
                Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 사용자 정보 조회
     */
    @Operation(
        summary = "특정 사용자 정보 조회",
        description = "사용자 ID로 특정 사용자의 공개 프로필 정보를 조회합니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> getUserById(
            @PathVariable Long id) {
        UserDto.ProfileResponse response = userService.getUserProfile(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 프로필 업데이트
     */
    @Operation(
        summary = "프로필 업데이트",
        description = "현재 로그인한 사용자의 프로필 정보를 업데이트합니다.",
        tags = {"사용자 관리"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        UserDto.ProfileResponse response = userService.updateProfile(
                Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 비밀번호 변경
     */
    @Operation(
        summary = "비밀번호 변경",
        description = "현재 로그인한 사용자의 비밀번호를 변경합니다.",
        tags = {"사용자 관리"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.PasswordChangeRequest request) {
        userService.changePassword(
                Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.ok(ApiResponse.successMessage("요청이 성공적으로 처리되었습니다."));
    }

    /**
     * 특정 사용자의 글 목록 조회
     */
    @Operation(
        summary = "사용자 작성 글 목록 조회",
        description = "특정 사용자가 작성한 글 목록을 페이징하여 조회합니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
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
    @Operation(
        summary = "사용자 작성 댓글 목록 조회",
        description = "특정 사용자가 작성한 댓글 목록을 페이징하여 조회합니다.",
        tags = {"사용자 관리"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
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
    @Operation(
        summary = "포인트 충전 (테스트용)",
        description = "테스트 목적으로 사용자 포인트를 충전합니다.",
        tags = {"사용자 관리"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "포인트 충전 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/me/points")
    public ResponseEntity<ApiResponse<UserDto.ProfileResponse>> addPoints(
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDto.PointRequest request) {
        UserDto.ProfileResponse response = userService.addPoints(
                Long.parseLong(userDetails.getUsername()), request.getPoints());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
