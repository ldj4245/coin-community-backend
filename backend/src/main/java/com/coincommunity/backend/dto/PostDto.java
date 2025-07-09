package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.entity.User;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 게시글 관련 DTO 클래스들
 */
public class PostDto {

    /**
     * 게시글 생성 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        private String content;

        @NotNull(message = "카테고리는 필수입니다")
        private PostCategory category;

        /**
         * CreateRequest를 Post 엔티티로 변환
         */
        public Post toEntity(User user) {
            return Post.builder()
                    .title(this.title)
                    .content(this.content)
                    .category(this.category)
                    .user(user)
                    .viewCount(0)
                    .likeCount(0)
                    .commentCount(0)
                    .build();
        }
    }

    /**
     * 게시글 수정 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다")
        private String title;

        @NotBlank(message = "내용은 필수입니다")
        private String content;
    }

    /**
     * 게시글 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostResponse {
        private Long id;
        private String title;
        private String content;
        private PostCategory category;
        private String categoryDisplayName;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private UserDto.UserResponse user;

        /**
         * Post 엔티티로부터 PostResponse DTO를 생성
         */
        public static PostResponse from(Post post) {
            return PostResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .category(post.getCategory())
                    .categoryDisplayName(post.getCategory().getDisplayName())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .user(UserDto.UserResponse.from(post.getUser()))
                    .build();
        }
    }

    /**
     * 게시글 요약 정보 DTO (목록 조회용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostSummary {
        private Long id;
        private String title;
        private PostCategory category;
        private String categoryDisplayName;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private LocalDateTime createdAt;
        private String username;

        /**
         * Post 엔티티로부터 PostSummary DTO를 생성
         */
        public static PostSummary from(Post post) {
            return PostSummary.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .category(post.getCategory())
                    .categoryDisplayName(post.getCategory().getDisplayName())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .createdAt(post.getCreatedAt())
                    .username(post.getUser().getUsername())
                    .build();
        }
    }
}