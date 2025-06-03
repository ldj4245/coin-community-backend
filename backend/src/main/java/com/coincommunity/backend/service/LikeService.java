package com.coincommunity.backend.service;

import com.coincommunity.backend.entity.Comment;
import com.coincommunity.backend.entity.CommentLike;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostLike;
import com.coincommunity.backend.entity.User;
import com.coincommunity.backend.repository.CommentLikeRepository;
import com.coincommunity.backend.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 및 댓글 좋아요 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {
    
    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostService postService;
    private final CommentService commentService;
    private final UserService userService;
    
    /**
     * 게시글 좋아요 상태를 토글합니다.
     * @return 토글 후 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)
     */
    @Transactional
    public boolean togglePostLike(Long postId, Long userId) {
        User user = userService.findById(userId);
        Post post = postService.findById(postId);
        
        boolean liked = false;
        
        // 이미 좋아요를 눌렀는지 확인
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            // 좋아요 취소
            postLikeRepository.findByUserIdAndPostId(userId, postId)
                    .ifPresent(postLikeRepository::delete);
        } else {
            // 좋아요 추가
            PostLike postLike = new PostLike(user, post);
            postLikeRepository.save(postLike);
            liked = true;
            
            // 게시글 작성자에게 포인트 추가
            if (!post.getUser().getId().equals(userId)) {
                userService.addPoints(post.getUser().getId(), 1);
            }
        }
        
        // 좋아요 개수 업데이트
        long likeCount = postLikeRepository.countByPostId(postId);
        post.updateLikeCount((int) likeCount);
        
        return liked;
    }
    
    /**
     * 댓글 좋아요 상태를 토글합니다.
     * @return 토글 후 좋아요 상태 (true: 좋아요 추가됨, false: 좋아요 취소됨)
     */
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        User user = userService.findById(userId);
        Comment comment = commentService.findById(commentId);
        
        boolean liked = false;
        
        // 이미 좋아요를 눌렀는지 확인
        if (commentLikeRepository.existsByUserIdAndCommentId(userId, commentId)) {
            // 좋아요 취소
            commentLikeRepository.findByUserIdAndCommentId(userId, commentId)
                    .ifPresent(commentLikeRepository::delete);
        } else {
            // 좋아요 추가
            CommentLike commentLike = new CommentLike(user, comment);
            commentLikeRepository.save(commentLike);
            liked = true;
            
            // 댓글 작성자에게 포인트 추가
            if (!comment.getUser().getId().equals(userId)) {
                userService.addPoints(comment.getUser().getId(), 1);
            }
        }
        
        // 좋아요 개수 업데이트
        long likeCount = commentLikeRepository.countByCommentId(commentId);
        comment.updateLikeCount((int) likeCount);
        
        return liked;
    }
    
    /**
     * 사용자가 게시글에 좋아요를 눌렀는지 확인합니다.
     */
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }
    
    /**
     * 사용자가 댓글에 좋아요를 눌렀는지 확인합니다.
     */
    public boolean isCommentLikedByUser(Long commentId, Long userId) {
        return commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
    }
}
