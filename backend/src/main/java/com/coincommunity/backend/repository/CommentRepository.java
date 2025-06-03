package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 댓글 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * 특정 게시글의 최상위 댓글(대댓글이 아닌)을 조회합니다.
     */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsByPostId(@Param("postId") Long postId);
    
    /**
     * 특정 게시글의 모든 댓글을 페이징하여 조회합니다.
     */
    Page<Comment> findByPostId(Long postId, Pageable pageable);
    
    /**
     * 특정 사용자가 작성한 모든 댓글을 페이징하여 조회합니다.
     */
    Page<Comment> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 특정 댓글의 모든 대댓글을 조회합니다.
     */
    List<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId);
    
    /**
     * 특정 게시글의 댓글 개수를 조회합니다.
     */
    long countByPostId(Long postId);
}
