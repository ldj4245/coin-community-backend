package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시글 좋아요 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    /**
     * 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인합니다.
     */
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * 특정 게시글의 좋아요 개수를 조회합니다.
     */
    long countByPostId(Long postId);
    
    /**
     * 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인합니다.
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    
    /**
     * 특정 게시글의 좋아요를 모두 삭제합니다.
     */
    void deleteAllByPostId(Long postId);
}
