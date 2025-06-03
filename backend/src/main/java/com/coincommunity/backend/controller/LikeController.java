package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 좋아요 관련 API 엔드포인트
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "좋아요", description = "게시글과 댓글에 대한 좋아요 기능 API")
public class LikeController {

    private final LikeService likeService;

    /**
     * 게시글 좋아요 토글
     */
    @Operation(
        summary = "게시글 좋아요 토글",
        description = "게시글의 좋아요를 토글합니다. 이미 좋아요한 상태라면 취소하고, 좋아요하지 않은 상태라면 좋아요를 추가합니다.",
        tags = {"좋아요"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<Map<String, Object>>> togglePostLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean liked = likeService.togglePostLike(postId, userId);
        
        Map<String, Object> response = Map.of(
            "postId", postId,
            "liked", liked
        );
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 댓글 좋아요 토글
     */
    @Operation(
        summary = "댓글 좋아요 토글",
        description = "댓글의 좋아요를 토글합니다. 이미 좋아요한 상태라면 취소하고, 좋아요하지 않은 상태라면 좋아요를 추가합니다.",
        tags = {"좋아요"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<Map<String, Object>>> toggleCommentLike(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean liked = likeService.toggleCommentLike(commentId, userId);
        
        Map<String, Object> response = Map.of(
            "commentId", commentId,
            "liked", liked
        );
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 게시글 좋아요 상태 확인
     */
    @Operation(
        summary = "게시글 좋아요 상태 확인",
        description = "현재 로그인한 사용자가 해당 게시글에 좋아요를 했는지 확인합니다.",
        tags = {"좋아요"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/posts/{postId}/like")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<Map<String, Object>>> checkPostLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean liked = likeService.isPostLikedByUser(postId, userId);
        
        Map<String, Object> response = Map.of(
            "postId", postId,
            "liked", liked
        );
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 댓글 좋아요 상태 확인
     */
    @Operation(
        summary = "댓글 좋아요 상태 확인",
        description = "현재 로그인한 사용자가 해당 댓글에 좋아요를 했는지 확인합니다.",
        tags = {"좋아요"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses(value = {
    })
    @GetMapping("/comments/{commentId}/like")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<Map<String, Object>>> checkCommentLike(
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean liked = likeService.isCommentLikedByUser(commentId, userId);
        
        Map<String, Object> response = Map.of(
            "commentId", commentId,
            "liked", liked
        );
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }
}
