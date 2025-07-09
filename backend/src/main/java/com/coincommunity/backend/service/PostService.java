package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PostDto;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.BusinessException;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 게시글 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;

    /**
     * 게시글 생성
     */
    @Transactional
    public PostDto.PostResponse createPost(Long userId, PostDto.CreateRequest request) {
        User user = userService.getUserById(userId);
        
        Post post = request.toEntity(user);
        Post savedPost = postRepository.save(post);

        log.info("새 게시글이 생성되었습니다: {} (작성자: {})", savedPost.getTitle(), user.getUsername());
        return PostDto.PostResponse.from(savedPost);
    }

    /**
     * 게시글 조회 (조회수 증가)
     */
    @Transactional
    public PostDto.PostResponse getPost(Long postId) {
        Post post = getPostById(postId);
        post.increaseViewCount();
        postRepository.save(post);
        
        return PostDto.PostResponse.from(post);
    }

    /**
     * 게시글 목록 조회
     */
    public PageResponse<PostDto.PostSummary> getPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<PostDto.PostSummary> postSummaries = postPage.getContent().stream()
                .map(PostDto.PostSummary::from)
                .collect(Collectors.toList());

        return PageResponse.from(postPage, postSummaries);
    }

    /**
     * 카테고리별 게시글 목록 조회
     */
    public PageResponse<PostDto.PostSummary> getPostsByCategory(PostCategory category, Pageable pageable) {
        Page<Post> postPage = postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
        
        List<PostDto.PostSummary> postSummaries = postPage.getContent().stream()
                .map(PostDto.PostSummary::from)
                .collect(Collectors.toList());

        return PageResponse.from(postPage, postSummaries);
    }

    /**
     * 사용자별 게시글 목록 조회
     */
    public PageResponse<PostDto.PostSummary> getPostsByUser(Long userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        Page<Post> postPage = postRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        List<PostDto.PostSummary> postSummaries = postPage.getContent().stream()
                .map(PostDto.PostSummary::from)
                .collect(Collectors.toList());

        return PageResponse.from(postPage, postSummaries);
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public PostDto.PostResponse updatePost(Long userId, Long postId, PostDto.UpdateRequest request) {
        Post post = getPostById(postId);
        
        // 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException("해당 게시글에 대한 수정 권한이 없습니다");
        }
        
        post.update(request.getTitle(), request.getContent());
        Post updatedPost = postRepository.save(post);
        
        log.info("게시글이 수정되었습니다: {} (작성자: {})", updatedPost.getTitle(), post.getUser().getUsername());
        return PostDto.PostResponse.from(updatedPost);
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getPostById(postId);
        
        // 권한 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException("해당 게시글에 대한 삭제 권한이 없습니다");
        }
        
        postRepository.delete(post);
        log.info("게시글이 삭제되었습니다: {} (작성자: {})", post.getTitle(), post.getUser().getUsername());
    }

    /**
     * 게시글 검색
     */
    public PageResponse<PostDto.PostSummary> searchPosts(String keyword, Pageable pageable) {
        Page<Post> postPage = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByCreatedAtDesc(
                keyword, keyword, pageable);
        
        List<PostDto.PostSummary> postSummaries = postPage.getContent().stream()
                .map(PostDto.PostSummary::from)
                .collect(Collectors.toList());

        return PageResponse.from(postPage, postSummaries);
    }

    /**
     * 게시글 ID로 Post 엔티티 조회 (내부 사용)
     */
    private Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("게시글을 찾을 수 없습니다: " + postId));
    }
}