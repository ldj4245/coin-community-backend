package com.coincommunity.backend.dto;

import com.coincommunity.backend.entity.Comment;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 정보 관련 DTO 클래스
 */
public class CommentDto {

    /**
     * 댓글 생성 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "내용은 필수 입력값입니다")
        private String content;
        
        private Long parentId;
        
        /**
         * CreateRequest DTO로부터 Comment 엔티티를 생성합니다.
         */
        public Comment toEntity(User user, Post post, Comment parent) {
            Comment comment = new Comment();
            comment.setContent(content);
            comment.setUser(user);
            comment.setPost(post);
            comment.setParent(parent);
            return comment;
        }
    }
    
    /**
     * 댓글 수정 요청 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "내용은 필수 입력값입니다")
        private String content;
    }
    
    /**
     * 댓글 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private String content;
        private Integer likeCount;
        private boolean isDeleted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private UserDto.UserResponse user;
        private Long postId;
        private Long parentId;
        private List<CommentResponse> children;
        private boolean liked;
        
        /**
         * Comment 엔티티로부터 CommentResponse DTO를 생성합니다.
         */
        public static CommentResponse from(Comment comment) {
            if (comment == null) {
                return null;
            }
            
            // 삭제된 댓글은 내용을 감춥니다
            String displayContent = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
            
            // 대댓글 정보 변환
            List<CommentResponse> childResponses = comment.getChildren().stream()
                    .map(CommentResponse::from)
                    .collect(Collectors.toList());
            
            return CommentResponse.builder()
                    .id(comment.getId())
                    .content(displayContent)
                    .likeCount(comment.getLikeCount())
                    .isDeleted(comment.isDeleted())
                    .createdAt(comment.getCreatedAt())
                    .updatedAt(comment.getUpdatedAt())
                    .user(UserDto.UserResponse.from(comment.getUser()))
                    .postId(comment.getPost().getId())
                    .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                    .children(childResponses)
                    .liked(false)
                    .build();
        }
        
        /**
         * Comment 엔티티로부터 CommentResponse DTO를 생성하며, 좋아요 상태를 설정합니다.
         */
        public static CommentResponse from(Comment comment, boolean liked) {
            CommentResponse response = from(comment);
            if (response != null) {
                response.setLiked(liked);
            }
            return response;
        }
        
        /**
         * 좋아요 상태를 설정합니다.
         */
        public void setLiked(boolean liked) {
            this.liked = liked;
        }
    }
}
