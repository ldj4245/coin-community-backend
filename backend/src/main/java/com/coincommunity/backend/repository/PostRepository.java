package com.coincommunity.backend.repository;

import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글 정보에 대한 데이터 액세스 인터페이스
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    /**
     * 카테고리별 게시글을 페이징하여 조회합니다.
     */
    Page<Post> findByCategory(PostCategory category, Pageable pageable);
    
    /**
     * 특정 사용자가 작성한 게시글을 페이징하여 조회합니다.
     */
    Page<Post> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 제목에 키워드를 포함하는 게시글을 검색합니다.
     */
    Page<Post> findByTitleContaining(String keyword, Pageable pageable);
    
    /**
     * 제목 또는 내용에 키워드를 포함하는 게시글을 검색합니다.
     */
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 인기 게시글을 조회합니다.
     */
    @Query("SELECT p FROM Post p ORDER BY p.viewCount DESC, p.likeCount DESC, p.commentCount DESC")
    List<Post> findTopPosts(Pageable pageable);
    
    /**
     * 최근 게시글을 조회합니다.
     */
    List<Post> findTop10ByOrderByCreatedAtDesc();
    
    /**
     * 특정 사용자가 작성한 게시글 수를 조회합니다.
     */
    long countByUserId(Long userId);
    
    /**
     * 특정 사용자의 게시글을 List 형태로 조회합니다.
     */
    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findByUserId(@Param("userId") Long userId);
}
