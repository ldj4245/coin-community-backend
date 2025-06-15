package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PostDto;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 게시글 관련 API 엔드포인트
 * 기본 경로: /api/posts
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@Tag(name = "게시글", description = "게시글 생성, 조회, 수정, 삭제 관련 API")
public class PostController {

    private final PostService postService;

    /**
     * 모든 게시글 조회 (페이징)
     */
    @Operation(
        summary = "게시글 목록 조회",
        description = "모든 게시글을 페이지네이션으로 조회합니다. 카테고리 필터링이 가능합니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<PageResponse<PostDto.SummaryResponse>>> getAllPosts(
            @Parameter(description = "페이지 정보 (size, page, sort)") 
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable,
            @Parameter(description = "카테고리 필터") 
            @RequestParam(required = false) String category) {
        
        Page<PostDto.PostSummaryResponse> postSummaryPage;
        if (category != null && !category.isEmpty()) {
            try {
                PostCategory postCategory = PostCategory.valueOf(category.toUpperCase());
                postSummaryPage = postService.getPostsByCategory(postCategory, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 카테고리입니다: " + category);
            }
        } else {
            postSummaryPage = postService.getAllPosts(pageable);
        }
        Page<PostDto.SummaryResponse> page = postSummaryPage.map(PostDto.SummaryResponse::new);
        
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * 게시글 상세 조회
     */
    @Operation(
        summary = "게시글 상세 조회",
        description = "특정 게시글의 상세 내용을 조회합니다. 좋아요 여부도 표시됩니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/{id}")
    public ResponseEntity<com.coincommunity.backend.dto.ApiResponse<PostDto.DetailResponse>> getPostById(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = null;
        if (userDetails != null) {
            userId = Long.parseLong(userDetails.getUsername());
        }
        
        PostDto.PostResponse postResponse = postService.getPostDetail(id, userId);
        PostDto.DetailResponse response = new PostDto.DetailResponse(postResponse);
        return ResponseEntity.ok(com.coincommunity.backend.dto.ApiResponse.success(response));
    }

    /**
     * 게시글 생성
     */
    @Operation(
        summary = "게시글 생성",
        description = "새로운 게시글을 작성합니다. 유효한 카테고리 목록은 /posts/categories 엔드포인트에서 확인할 수 있습니다.",
        tags = {"게시글"},
        security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "게시글 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostDto.DetailResponse>> createPost(
            @Valid @RequestBody PostDto.CreateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        PostDto.PostResponse postResponse = postService.createPost(request, userId);
        PostDto.DetailResponse response = new PostDto.DetailResponse(postResponse);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 게시글 수정
     */
    @Operation(
        summary = "게시글 수정",
        description = "기존 게시글을 수정합니다. 작성자만 수정할 수 있습니다.",
        tags = {"게시글"},
        security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto.DetailResponse>> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @Valid @RequestBody PostDto.UpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        PostDto.PostResponse postResponse = postService.updatePost(id, request, userId);
        PostDto.DetailResponse response = new PostDto.DetailResponse(postResponse);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시글 삭제
     */
    @Operation(
        summary = "게시글 삭제",
        description = "게시글을 삭제합니다. 작성자만 삭제할 수 있습니다.",
        tags = {"게시글"},
        security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "Bearer")}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = Long.parseLong(userDetails.getUsername());
        postService.deletePost(id, userId);
        
        return ResponseEntity.ok(ApiResponse.successMessage("요청이 성공적으로 처리되었습니다."));
    }

    /**
     * 인기 게시글 목록 조회
     */
    @Operation(
        summary = "인기 게시글 목록 조회",
        description = "좋아요가 많은 인기 게시글 목록을 조회합니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PostDto.SummaryResponse>>> getPopularPosts(
            @Parameter(description = "조회할 게시글 수") @RequestParam(defaultValue = "10") int limit) {
        
        List<PostDto.PostSummaryResponse> postSummaryList = postService.getPopularPosts(limit);
        List<PostDto.SummaryResponse> posts = postSummaryList.stream()
                .map(PostDto.SummaryResponse::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 최근 게시글 목록 조회
     */
    @Operation(
        summary = "최근 게시글 목록 조회",
        description = "최근에 작성된 게시글 목록을 조회합니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<PostDto.SummaryResponse>>> getRecentPosts(
            @Parameter(description = "조회할 게시글 수") @RequestParam(defaultValue = "10") int limit) {
        
        List<PostDto.PostSummaryResponse> postSummaryList = postService.getRecentPosts(limit);
        List<PostDto.SummaryResponse> posts = postSummaryList.stream()
                .map(PostDto.SummaryResponse::new)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    /**
     * 게시글 검색
     */
    @Operation(
        summary = "게시글 검색",
        description = "키워드로 게시글을 검색합니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<PostDto.SummaryResponse>>> searchPosts(
            @Parameter(description = "검색 키워드") @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<PostDto.PostSummaryResponse> postSummaryPage = postService.searchPosts(keyword, pageable);
        Page<PostDto.SummaryResponse> page = postSummaryPage.map(PostDto.SummaryResponse::new);
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    /**
     * 모든 게시글 카테고리 조회
     */
    @Operation(
        summary = "게시글 카테고리 목록 조회",
        description = "모든 유효한 게시글 카테고리를 조회합니다.",
        tags = {"게시글"}
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getCategories() {
        List<Map<String, String>> categories = Arrays.stream(PostCategory.values())
                .map(category -> {
                    Map<String, String> categoryMap = new HashMap<>();
                    categoryMap.put("name", category.name());
                    categoryMap.put("displayName", category.getDisplayName());
                    return categoryMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
}
