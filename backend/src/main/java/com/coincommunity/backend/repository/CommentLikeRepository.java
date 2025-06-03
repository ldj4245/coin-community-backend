package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 댓글 좋아요 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    /**
     * 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인합니다.
     */
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
    
    /**
     * 특정 댓글의 좋아요 개수를 조회합니다.
     */
    long countByCommentId(Long commentId);
    
    /**
     * 특정 사용자가 특정 댓글에 좋아요를 눌렀는지 확인합니다.
     */
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    
    /**
     * 특정 댓글의 좋아요를 모두 삭제합니다.
     */
    void deleteAllByCommentId(Long commentId);
}
