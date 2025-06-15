package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.CommentDto;
import com.coincommunity.backend.entity.Comment;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.repository.CommentLikeRepository;
import com.coincommunity.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Collections;

/**
 * 댓글 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostService postService;
    private final UserService userService;
    
    /**
     * 댓글 ID로 댓글을 조회합니다.
     */
    public Comment findById(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + id));
    }
    
    /**
     * 특정 게시글의 모든 최상위 댓글과 그에 딸린 대댓글을 계층 구조로 조회합니다.
     */
    public List<CommentDto.CommentResponse> getCommentsByPost(Long postId, Long userId) {
        // 모든 댓글을 한 번에 조회하여 N+1 문제를 방지합니다.
        List<Comment> allComments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(postId);
        
        // 디버깅: 조회된 모든 댓글 정보 로그 출력
        System.out.println("=== 조회된 모든 댓글 정보 ===");
        for (Comment comment : allComments) {
            System.out.println(String.format("댓글 ID: %d, 내용: %s, 부모 ID: %s", 
                comment.getId(), 
                comment.getContent(), 
                comment.getParent() != null ? comment.getParent().getId() : "null"));
        }
        
        // 부모 ID별로 자식 댓글을 그룹화합니다.
        Map<Long, List<Comment>> childrenMap = allComments.stream()
                .filter(comment -> comment.getParent() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));
                
        // 디버깅: childrenMap 내용 로그 출력
        System.out.println("=== childrenMap 내용 ===");
        childrenMap.forEach((parentId, children) -> {
            System.out.println(String.format("부모 ID %d의 자식: %s", parentId, 
                children.stream().map(c -> c.getId().toString()).collect(Collectors.joining(", "))));
        });
        
        // 최상위 댓글만 필터링하여 계층 구조로 변환합니다.
        List<Comment> rootComments = allComments.stream()
                .filter(comment -> comment.getParent() == null)
                .collect(Collectors.toList());
                
        // 디버깅: 최상위 댓글 정보 로그 출력
        System.out.println("=== 최상위 댓글 ===");
        for (Comment root : rootComments) {
            System.out.println(String.format("최상위 댓글 ID: %d, 내용: %s", root.getId(), root.getContent()));
        }
        
        return rootComments.stream()
                .map(root -> buildCommentResponse(root, childrenMap, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 댓글 엔티티를 재귀적으로 CommentResponse DTO로 변환합니다.
     */
    private CommentDto.CommentResponse buildCommentResponse(Comment comment, Map<Long, List<Comment>> childrenMap, Long userId) {
        // 좋아요 상태 확인
        boolean liked = userId != null && commentLikeRepository.existsByUserIdAndCommentId(userId, comment.getId());
        // 삭제된 댓글 내용 처리
        String displayContent = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        // 자식 댓글 재귀 변환
        List<CommentDto.CommentResponse> childResponses = childrenMap.getOrDefault(comment.getId(), Collections.emptyList())
                .stream()
                .map(child -> buildCommentResponse(child, childrenMap, userId))
                .collect(Collectors.toList());
        // 응답 DTO 생성
        return CommentDto.CommentResponse.builder()
                .id(comment.getId())
                .content(displayContent)
                .likeCount(comment.getLikeCount())
                .isDeleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .user(comment.getUser() != null ? com.coincommunity.backend.dto.UserDto.UserResponse.from(comment.getUser()) : null)
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .children(childResponses)
                .liked(liked)
                .build();
    }
    
    /**
     * 새로운 댓글을 작성합니다.
     */
    @Transactional
    public CommentDto.CommentResponse createComment(Long postId, CommentDto.CreateRequest requestDto, Long userId) {
        // 디버깅: 댓글 생성 요청 정보 로그
        System.out.println("=== 댓글 생성 요청 ===");
        System.out.println(String.format("postId: %d, userId: %d, parentId: %s, content: %s", 
            postId, userId, requestDto.getParentId(), requestDto.getContent()));
            
        Post post = postService.findById(postId);
        User user = userService.findById(userId);

        Comment parentComment = null;
        if (requestDto.getParentId() != null) {
            parentComment = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("상위 댓글을 찾을 수 없습니다. ID: " + requestDto.getParentId()));
            System.out.println(String.format("부모 댓글 찾음: ID %d, 내용: %s", parentComment.getId(), parentComment.getContent()));
        } else {
            System.out.println("최상위 댓글로 생성");
        }

        Comment comment = requestDto.toEntity(user, post, parentComment);
        
        // 디버깅: 엔티티 생성 후 정보 확인
        System.out.println(String.format("생성된 엔티티 - 부모: %s, 내용: %s", 
            comment.getParent() != null ? comment.getParent().getId() : "null", comment.getContent()));
            
        Comment savedComment = commentRepository.save(comment);
        
        // 디버깅: 저장 후 정보 확인
        System.out.println(String.format("저장된 댓글 - ID: %d, 부모: %s, 내용: %s", 
            savedComment.getId(), 
            savedComment.getParent() != null ? savedComment.getParent().getId() : "null", 
            savedComment.getContent()));
        
        return CommentDto.CommentResponse.from(savedComment, false);
    }
    
    /**
     * 댓글을 수정합니다.
     */
    @Transactional
    public CommentDto.CommentResponse updateComment(Long commentId, CommentDto.UpdateRequest requestDto, Long userId) {
        Comment comment = getCommentEntityById(commentId);

        // 댓글 작성자 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글을 수정할 권한이 없습니다.");
        }

        comment.updateContent(requestDto.getContent());
        Comment updatedComment = commentRepository.save(comment);

        boolean liked = false;
        if (userId != null) {
            liked = commentLikeRepository.existsByUserIdAndCommentId(userId, updatedComment.getId());
        }

        return CommentDto.CommentResponse.from(updatedComment, liked);
    }
    
    /**
     * 댓글을 삭제합니다(soft delete).
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getCommentEntityById(commentId);

        // 댓글 작성자 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }
    
    /**
     * 댓글 ID로 댓글 엔티티를 조회합니다.
     */
    public Comment getCommentEntityById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("댓글을 찾을 수 없습니다. ID: " + commentId));
    }
    
    /**
     * 특정 사용자가 작성한 모든 댓글을 페이지네이션하여 조회합니다.
     */
    public Page<CommentDto.CommentResponse> getCommentsByUser(Long userId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByUserId(userId, pageable);
        return comments.map(comment -> {
            boolean liked = commentLikeRepository.existsByUserIdAndCommentId(userId, comment.getId());
            return CommentDto.CommentResponse.from(comment, liked);
        });
    }
}
