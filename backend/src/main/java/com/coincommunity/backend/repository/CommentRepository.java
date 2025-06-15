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
     * 특정 게시글의 모든 댓글을 계층 구조로 조회합니다. (대댓글 포함)
     * JPQL의 fetch join을 사용하여 N+1 문제를 해결합니다.
     * 
     * 주의: 이 쿼리는 모든 계층의 대댓글을 함께 로드합니다.
     */
    @Query("SELECT DISTINCT c FROM Comment c LEFT JOIN FETCH c.children child1 LEFT JOIN FETCH child1.children child2 WHERE c.post.id = :postId AND c.parent IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findRootCommentsWithChildrenByPostId(@Param("postId") Long postId);
    
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
    
    /**
     * 특정 게시글의 모든 댓글을 생성일 기준으로 조회합니다.
     */
    List<Comment> findAllByPostIdOrderByCreatedAtAsc(Long postId);
}
