package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 정보 관련 DTO 클래스
 */
public class PostDto {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 게시글 생성 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "제목은 필수 입력값입니다")
        private String title;
        
        @NotBlank(message = "내용은 필수 입력값입니다")
        private String content;
        
        @NotNull(message = "카테고리는 필수 입력값입니다")
        private PostCategory category;
        
        private List<String> imageUrls;
        
        /**
         * CreateRequest DTO로부터 Post 엔티티를 생성합니다.
         */
        public Post toEntity(User user) {
            Post post = new Post();
            post.setTitle(this.title);
            post.setContent(this.content);
            post.setCategory(this.category);
            post.setUser(user);
            
            if (imageUrls != null && !imageUrls.isEmpty()) {
                try {
                    post.setImageUrls(objectMapper.writeValueAsString(imageUrls));
                } catch (JsonProcessingException e) {
                    post.setImageUrls("[]");
                }
            } else {
                post.setImageUrls("[]");
            }
            
            return post;
        }
    }
    
    /**
     * 게시글 수정 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "제목은 필수 입력값입니다")
        private String title;
        
        @NotBlank(message = "내용은 필수 입력값입니다")
        private String content;
        
        private List<String> imageUrls;
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
        private List<String> imageUrls;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private UserDto.UserResponse user;
        private boolean liked;
        
        /**
         * Post 엔티티로부터 PostResponse DTO를 생성합니다.
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
                    .imageUrls(parseImageUrls(post.getImageUrls()))
                    .createdAt(post.getCreatedAt())
                    .updatedAt(post.getUpdatedAt())
                    .user(UserDto.UserResponse.from(post.getUser()))
                    .liked(false)
                    .build();
        }
        
        /**
         * Post 엔티티로부터 PostResponse DTO를 생성하며, 좋아요 상태를 설정합니다.
         */
        public static PostResponse from(Post post, boolean liked) {
            PostResponse response = from(post);
            response.setLiked(liked);
            return response;
        }
        
        /**
         * 좋아요 상태를 설정합니다.
         */
        public void setLiked(boolean liked) {
            this.liked = liked;
        }
        
        /**
         * 이미지 URL JSON 문자열을 List<String>으로 변환합니다.
         */
        private static List<String> parseImageUrls(String imageUrlsJson) {
            if (imageUrlsJson == null || imageUrlsJson.isEmpty()) {
                return new ArrayList<>();
            }
            
            try {
                return objectMapper.readValue(
                    imageUrlsJson,
                    new TypeReference<List<String>>() {}
                );
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
        }
    }
    
    /**
     * 게시글 목록 응답 DTO (요약 정보)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostSummaryResponse {
        private Long id;
        private String title;
        private PostCategory category;
        private String categoryDisplayName;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private LocalDateTime createdAt;
        private UserDto.UserResponse user;
        
        /**
         * Post 엔티티로부터 PostSummaryResponse DTO를 생성합니다.
         */
        public static PostSummaryResponse from(Post post) {
            return PostSummaryResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .category(post.getCategory())
                    .categoryDisplayName(post.getCategory().getDisplayName())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount())
                    .createdAt(post.getCreatedAt())
                    .user(UserDto.UserResponse.from(post.getUser()))
                    .build();
        }
    }
    
    /**
     * 게시글 상세 응답 DTO (DetailResponse 별칭)
     */
    public static class DetailResponse extends PostResponse {
        /**
         * Post 엔티티로부터 DetailResponse DTO를 생성합니다.
         */
        public static DetailResponse from(Post post) {
            PostResponse postResponse = PostResponse.from(post);
            return (DetailResponse) postResponse;
        }
        
        /**
         * Post 엔티티로부터 DetailResponse DTO를 생성하며, 좋아요 상태를 설정합니다.
         */
        public static DetailResponse from(Post post, boolean liked) {
            PostResponse postResponse = PostResponse.from(post, liked);
            return (DetailResponse) postResponse;
        }
    }
    
    /**
     * 게시글 요약 응답 DTO (SummaryResponse 별칭)  
     */
    public static class SummaryResponse extends PostSummaryResponse {
        /**
         * Post 엔티티로부터 SummaryResponse DTO를 생성합니다.
         */
        public static SummaryResponse from(Post post) {
            PostSummaryResponse postSummaryResponse = PostSummaryResponse.from(post);
            return (SummaryResponse) postSummaryResponse;
        }
    }
}
