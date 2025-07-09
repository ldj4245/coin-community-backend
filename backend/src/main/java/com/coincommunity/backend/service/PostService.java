package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.PostDto;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.PostRepository;
import com.coincommunity.backend.repository.PostLikeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    
    /**
     * 게시글 ID로 게시글을 조회합니다.
     */
    public Post findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 게시글 ID로 게시글 상세 정보를 조회합니다.
     */
    public PostDto.PostResponse getPostDetail(Long id, Long userId) {
        Post post = findById(id);
        
        // 조회수 증가
        increaseViewCount(post);
        
        boolean liked = false;
        if (userId != null) {
            liked = postLikeRepository.existsByUserIdAndPostId(userId, id);
        }
        
        return PostDto.PostResponse.from(post, liked);
    }
    
    /**
     * 조회수를 증가시킵니다.
     */
    @Transactional
    public void increaseViewCount(Post post) {
        post.increaseViewCount();
        postRepository.save(post);
    }
    
    /**
     * 새 게시글을 작성합니다.
     */
    @Transactional
    public PostDto.PostResponse createPost(PostDto.CreateRequest request, Long userId) {
        User user = userService.findById(userId);
        Post post = request.toEntity(user);
        Post savedPost = postRepository.save(post);
        
        // 포인트 증가
        userService.addPoints(userId, 5);
        
        return PostDto.PostResponse.from(savedPost);
    }
    
    /**
     * 게시글을 수정합니다.
     */
    @Transactional
    public PostDto.PostResponse updatePost(Long postId, PostDto.UpdateRequest request, Long userId) {
        Post post = findById(postId);
        
        // 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 게시글에 대한 수정 권한이 없습니다.");
        }
        
        post.update(request.getTitle(), request.getContent());
        Post updatedPost = postRepository.save(post);
        
        return PostDto.PostResponse.from(updatedPost);
    }
    
    /**
     * 게시글을 삭제합니다.
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findById(postId);
        
        // 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 게시글에 대한 삭제 권한이 없습니다.");
        }
        
        postRepository.delete(post);
    }
    
    /**
     * 카테고리별 게시글 목록을 조회합니다.
     */
    public Page<PostDto.PostSummaryResponse> getPostsByCategory(PostCategory category, Pageable pageable) {
        Page<Post> posts = postRepository.findByCategory(category, pageable);
        return posts.map(PostDto.PostSummaryResponse::from);
    }
    
    /**
     * 전체 게시글 목록을 조회합니다.
     */
    public Page<PostDto.PostSummaryResponse> getAllPosts(Pageable pageable) {
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.map(PostDto.PostSummaryResponse::from);
    }
    
    /**
     * 특정 사용자가 작성한 게시글 목록을 조회합니다.
     */
    public Page<PostDto.PostSummaryResponse> getPostsByUser(Long userId, Pageable pageable) {
        Page<Post> posts = postRepository.findByUserId(userId, pageable);
        return posts.map(PostDto.PostSummaryResponse::from);
    }
    
    /**
     * 키워드로 게시글을 검색합니다.
     */
    public Page<PostDto.PostSummaryResponse> searchPosts(String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchByKeyword(keyword, pageable);
        return posts.map(PostDto.PostSummaryResponse::from);
    }
    
    /**
     * 인기 게시글 목록을 조회합니다.
     */
    public List<PostDto.PostSummaryResponse> getPopularPosts(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Post> posts = postRepository.findTopPosts(pageable);
        return posts.stream()
                .map(PostDto.PostSummaryResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 최근 게시글 목록을 조회합니다.
     */
    public List<PostDto.PostSummaryResponse> getRecentPosts(int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        List<Post> posts = postRepository.findTopPosts(pageable);
        return posts.stream()
                .map(PostDto.PostSummaryResponse::from)
                .collect(Collectors.toList());
    }
}
