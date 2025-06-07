package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.entity.UserRole;
import com.coincommunity.backend.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 사용자 정보 관련 DTO 클래스
 */
public class UserDto {

    /**
     * 회원 가입 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    public static class SignupRequest {
        @NotBlank(message = "사용자명은 필수 입력값입니다")
        @Size(min = 3, max = 20, message = "사용자명은 3~20자 사이여야 합니다")
        private String username;
        
        @NotBlank(message = "이메일은 필수 입력값입니다")
        @Email(message = "이메일 형식에 맞지 않습니다")
        private String email;
        
        @NotBlank(message = "비밀번호는 필수 입력값입니다")
        @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
        private String password;
        
        private String nickname;

        @JsonCreator
        public SignupRequest(@JsonProperty("username") String username, 
                             @JsonProperty("email") String email, 
                             @JsonProperty("password") String password, 
                             @JsonProperty("nickname") String nickname) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.nickname = nickname;
        }
    }
    
    /**
     * 로그인 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "사용자명 또는 이메일을 입력해주세요")
        private String usernameOrEmail;
        
        @NotBlank(message = "비밀번호를 입력해주세요")
        private String password;

        @JsonCreator
        public LoginRequest(@JsonProperty("usernameOrEmail") String usernameOrEmail, 
                            @JsonProperty("password") String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }
    }
    
    /**
     * 로그인 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private UserResponse user;
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
        private String avatarUrl;
        private UserRole role;
        private UserStatus status;
        private Integer activityLevel;
        private Integer point;
        
        /**
         * User 엔티티로부터 UserResponse DTO를 생성합니다.
         */
        public static UserResponse from(User user) {
            return UserResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .activityLevel(user.getActivityLevel())
                    .point(user.getPoint())
                    .build();
        }
    }
    
    /**
     * 프로필 업데이트 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String nickname;
        private String avatarUrl;
    }
    
    /**
     * 비밀번호 변경 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "현재 비밀번호를 입력해주세요")
        private String currentPassword;
        
        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Size(min = 6, message = "비밀번호는 최소 6자 이상이어야 합니다")
        private String newPassword;
    }
    
    /**
     * 회원 가입 요청 DTO (RegisterRequest 별칭)
     */
    public static class RegisterRequest extends SignupRequest {
    }
    
    /**
     * 로그인 응답 DTO (LoginResponse 별칭)
     */
    public static class LoginResponse extends AuthResponse {
        public LoginResponse(String accessToken, String tokenType, UserResponse user) {
            super(accessToken, tokenType, user);
        }
    }
    
    /**
     * 프로필 응답 DTO (ProfileResponse 별칭)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResponse {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatarUrl;
        private UserRole role;
        private UserStatus status;
        private Integer activityLevel;
        private Integer point;
        
        /**
         * User 엔티티로부터 ProfileResponse DTO를 생성합니다.
         */
        public static ProfileResponse from(User user) {
            return ProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .activityLevel(user.getActivityLevel())
                    .point(user.getPoint())
                    .build();
        }
    }
    
    /**
     * 프로필 업데이트 요청 DTO (UpdateRequest 별칭)
     */
    public static class UpdateRequest extends UpdateProfileRequest {
    }
    
    /**
     * 비밀번호 변경 요청 DTO (PasswordChangeRequest 별칭)
     */
    public static class PasswordChangeRequest extends ChangePasswordRequest {
    }
    
    /**
     * 포인트 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointRequest {
        private Integer points;
    }
    
    /**
     * 게시글 요약 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostSummaryResponse {
        private Long id;
        private String title;
        private String category;
        private String categoryDisplayName;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private java.time.LocalDateTime createdAt;
    }
    
    /**
     * 댓글 요약 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentSummaryResponse {
        private Long id;
        private String content;
        private java.time.LocalDateTime createdAt;
        private Long postId;
        private String postTitle;
    }
    
    /**
     * 사용자 요약 정보 DTO (Summary)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String username;
        private String nickname;
        private String avatarUrl;
        private UserRole role;
        private Integer activityLevel;
        
        /**
         * User 엔티티로부터 Summary DTO를 생성합니다.
         */
        public static Summary from(User user) {
            return Summary.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .role(user.getRole())
                    .activityLevel(user.getActivityLevel())
                    .build();
        }
    }
}
