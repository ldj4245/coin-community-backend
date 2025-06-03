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
import java.util.stream.Collectors;

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
     * 특정 게시글의 모든 최상위 댓글과 그에 딸린 대댓글을 조회합니다.
     */
    public List<CommentDto.CommentResponse> getCommentsByPost(Long postId, Long userId) {
        List<Comment> rootComments = commentRepository.findRootCommentsByPostId(postId);
        
        return rootComments.stream()
                .map(comment -> convertToCommentResponseWithLikeStatus(comment, userId))
                .collect(Collectors.toList());
    }
    
    /**
     * 새로운 댓글을 작성합니다.
     */
    @Transactional
    public CommentDto.CommentResponse createComment(Long postId, CommentDto.CreateRequest request, Long userId) {
        User user = userService.findById(userId);
        Post post = postService.findById(postId);
        
        // 대댓글인 경우 부모 댓글 조회
        Comment parent = null;
        if (request.getParentId() != null) {
            parent = findById(request.getParentId());
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("부모 댓글의 게시글과 현재 게시글이 일치하지 않습니다.");
            }
        }
        
        // 댓글 생성
        Comment comment = request.toEntity(user, post, parent);
        Comment savedComment = commentRepository.save(comment);
        
        // 게시글 댓글 수 업데이트
        post.updateCommentCount();
        
        // 포인트 증가
        userService.addPoints(userId, 1);
        
        return CommentDto.CommentResponse.from(savedComment);
    }
    
    /**
     * 댓글을 수정합니다.
     */
    @Transactional
    public CommentDto.CommentResponse updateComment(Long commentId, CommentDto.UpdateRequest request, Long userId) {
        Comment comment = findById(commentId);
        
        // 권한 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 댓글에 대한 수정 권한이 없습니다.");
        }
        
        // 삭제된 댓글인지 확인
        if (comment.isDeleted()) {
            throw new IllegalArgumentException("이미 삭제된 댓글은 수정할 수 없습니다.");
        }
        
        comment.updateContent(request.getContent());
        Comment updatedComment = commentRepository.save(comment);
        
        return CommentDto.CommentResponse.from(updatedComment);
    }
    
    /**
     * 댓글을 삭제합니다(soft delete).
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = findById(commentId);
        
        // 권한 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 댓글에 대한 삭제 권한이 없습니다.");
        }
        
        // 대댓글이 있는 경우 soft delete, 아니면 물리적 삭제
        if (comment.hasChildren()) {
            comment.delete();
            commentRepository.save(comment);
        } else {
            // 대댓글인 경우 부모 댓글의 children 목록에서도 제거
            if (comment.getParent() != null) {
                comment.getParent().getChildren().remove(comment);
            }
            commentRepository.delete(comment);
        }
        
        // 게시글 댓글 수 업데이트
        Post post = comment.getPost();
        post.updateCommentCount();
    }
    
    /**
     * Comment 엔티티를 CommentResponse DTO로 변환하면서 좋아요 상태를 설정합니다.
     */
    private CommentDto.CommentResponse convertToCommentResponseWithLikeStatus(Comment comment, Long userId) {
        boolean liked = false;
        if (userId != null) {
            liked = commentLikeRepository.existsByUserIdAndCommentId(userId, comment.getId());
        }
        
        return CommentDto.CommentResponse.from(comment, liked);
    }
    
    /**
     * 특정 사용자가 작성한 댓글 목록을 조회합니다.
     */
    public Page<CommentDto.CommentResponse> getCommentsByUser(Long userId, Pageable pageable) {
        Page<Comment> comments = commentRepository.findByUserId(userId, pageable);
        return comments.map(comment -> CommentDto.CommentResponse.from(comment, false));
    }
}
