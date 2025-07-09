package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.entity.UserRole;
import com.coincommunity.backend.entity.UserStatus;
import lombok.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 사용자 관련 DTO 클래스들
 */
public class UserDto {

    /**
     * 사용자 등록 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "사용자명은 필수입니다")
        @Size(min = 4, max = 20, message = "사용자명은 4-20자 사이여야 합니다")
        private String username;

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 6, max = 100, message = "비밀번호는 6-100자 사이여야 합니다")
        private String password;

        private String nickname;

        /**
         * RegisterRequest를 User 엔티티로 변환
         */
        public User toEntity(String encodedPassword) {
            return User.builder()
                    .username(this.username)
                    .email(this.email)
                    .password(encodedPassword)
                    .nickname(this.nickname != null ? this.nickname : this.username)
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .emailVerified(false)
                    .build();
        }
    }

    /**
     * 사용자 로그인 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "사용자명은 필수입니다")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다")
        private String password;
    }

    /**
     * 사용자 정보 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String profileImageUrl;
        private UserRole role;
        private UserStatus status;
        private LocalDateTime createdAt;

        /**
         * User 엔티티로부터 UserResponse DTO를 생성
         */
        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .profileImageUrl(user.getProfileImageUrl())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    /**
     * 로그인 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private UserResponse user;
    }

    /**
     * 프로필 업데이트 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String nickname;
        private String profileImageUrl;
    }
}