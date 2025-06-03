package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.CommentDto;
import com.coincommunity.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 관련 API 엔드포인트
 * 기본 경로: /api/posts/{postId}/comments
 */
@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "댓글", description = "댓글 작성, 조회, 수정, 삭제 관련 API")
public class CommentController {

    private final CommentService commentService;

    /**
     * 특정 게시글의 모든 댓글 조회
     */
    @Operation(
        summary = "댓글 목록 조회",
        description = "특정 게시글에 작성된 모든 댓글을 조회합니다. 로그인한 사용자의 경우 자신의 댓글 여부가 표시됩니다.",
        tags = {"댓글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<List<CommentDto.CommentResponse>>> getCommentsByPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = null;
        if (userDetails != null) {
            userId = Long.parseLong(userDetails.getUsername());
        }
        
        List<CommentDto.CommentResponse> comments = commentService.getCommentsByPost(postId, userId);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(comments));
    }

    /**
     * 새로운 댓글 작성
     */
    @Operation(
        summary = "댓글 작성",
        description = "특정 게시글에 새 댓글을 작성합니다. 부모 댓글 ID를 포함하면 다중 계층 댓글로 작성됩니다.",
        tags = {"댓글"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "댓글 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<CommentDto.CommentResponse>> createComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody CommentDto.CreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        CommentDto.CommentResponse response = commentService.createComment(postId, request, userId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 댓글 수정
     */
    @Operation(
        summary = "댓글 수정",
        description = "작성한 댓글을 수정합니다. 자신이 작성한 댓글만 수정할 수 있습니다.",
        tags = {"댓글"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{commentId}")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<CommentDto.CommentResponse>> updateComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentDto.UpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        CommentDto.CommentResponse response = commentService.updateComment(commentId, request, userId);
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 댓글 삭제
     */
    @Operation(
        summary = "댓글 삭제",
        description = "작성한 댓글을 삭제합니다. 자신이 작성한 댓글만 삭제할 수 있습니다.",
        tags = {"댓글"},
        security = {@SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<Void>> deleteComment(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Parameter(description = "댓글 ID") @PathVariable Long commentId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        commentService.deleteComment(commentId, userId);
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.successMessage("요청이 성공적으로 처리되었습니다."));
    }
}
