package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PostDto;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.service.PostService;
import com.coincommunity.backend.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 게시글 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "게시글 관리 API")
public class PostController {

    private final PostService postService;

    /**
     * 게시글 생성
     */
    @PostMapping
    @Operation(summary = "게시글 생성", description = "새로운 게시글을 생성합니다.")
    public ResponseEntity<ApiResponse<PostDto.PostResponse>> createPost(
            @Valid @RequestBody PostDto.CreateRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PostDto.PostResponse response = postService.createPost(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시글 조회
     */
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 조회", description = "특정 게시글을 조회합니다. 조회수가 증가합니다.")
    public ResponseEntity<ApiResponse<PostDto.PostResponse>> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        
        PostDto.PostResponse response = postService.getPost(postId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시글 목록 조회
     */
    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "게시글 목록을 페이지네이션으로 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.PostSummary>>> getPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        
        PageResponse<PostDto.PostSummary> response = postService.getPosts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 카테고리별 게시글 목록 조회
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "카테고리별 게시글 조회", description = "특정 카테고리의 게시글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.PostSummary>>> getPostsByCategory(
            @Parameter(description = "게시글 카테고리") @PathVariable PostCategory category,
            @PageableDefault(size = 20) Pageable pageable) {
        
        PageResponse<PostDto.PostSummary> response = postService.getPostsByCategory(category, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자별 게시글 목록 조회
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "사용자별 게시글 조회", description = "특정 사용자가 작성한 게시글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.PostSummary>>> getPostsByUser(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        PageResponse<PostDto.PostSummary> response = postService.getPostsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다. 작성자만 수정할 수 있습니다.")
    public ResponseEntity<ApiResponse<PostDto.PostResponse>> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody PostDto.UpdateRequest request) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PostDto.PostResponse response = postService.updatePost(currentUserId, postId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시글 삭제
     */
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다. 작성자만 삭제할 수 있습니다.")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        postService.deletePost(currentUserId, postId);
        return ResponseEntity.ok(ApiResponse.successMessage("게시글이 삭제되었습니다"));
    }

    /**
     * 게시글 검색
     */
    @GetMapping("/search")
    @Operation(summary = "게시글 검색", description = "제목 또는 내용으로 게시글을 검색합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.PostSummary>>> searchPosts(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        
        PageResponse<PostDto.PostSummary> response = postService.searchPosts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내가 작성한 게시글 목록 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 게시글 조회", description = "현재 로그인한 사용자가 작성한 게시글 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.PostSummary>>> getMyPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        PageResponse<PostDto.PostSummary> response = postService.getPostsByUser(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}