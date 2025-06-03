package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    private Integer likeCount = 0;
    
    private boolean isDeleted = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentLike> likes = new ArrayList<>();
    
    /**
     * 댓글 내용을 업데이트합니다.
     */
    public void updateContent(String content) {
        this.content = content;
    }
    
    /**
     * 댓글 좋아요 개수를 업데이트합니다.
     */
    public void updateLikeCount(int count) {
        this.likeCount = count;
    }
    
    /**
     * 댓글을 삭제 처리합니다. (soft delete)
     */
    public void delete() {
        this.isDeleted = true;
    }
    
    /**
     * 댓글을 복원합니다.
     */
    public void restore() {
        this.isDeleted = false;
    }
    
    /**
     * 댓글이 대댓글인지 확인합니다.
     */
    public boolean isChildComment() {
        return this.parent != null;
    }
    
    /**
     * 댓글이 대댓글을 가지고 있는지 확인합니다.
     */
    public boolean hasChildren() {
        return !this.children.isEmpty();
    }
}
