package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;
    
    @Builder.Default
    private Integer viewCount = 0;
    @Builder.Default
    private Integer likeCount = 0;
    @Builder.Default
    private Integer commentCount = 0;
    
    @Column(columnDefinition = "TEXT")
    private String imageUrls; // JSON 형태로 저장
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
    
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostLike> likes = new ArrayList<>();
    
    /**
     * 조회수를 증가시킵니다.
     */
    public void increaseViewCount() {
        this.viewCount += 1;
    }
    
    /**
     * 좋아요 개수를 업데이트합니다.
     */
    public void updateLikeCount(int count) {
        this.likeCount = count;
    }
    
    /**
     * 댓글 개수를 업데이트합니다.
     */
    public void updateCommentCount() {
        this.commentCount = this.comments.size();
    }
    
    /**
     * 게시글 내용을 업데이트합니다.
     */
    public void update(String title, String content, String imageUrls) {
        this.title = title;
        this.content = content;
        this.imageUrls = imageUrls;
    }
}
