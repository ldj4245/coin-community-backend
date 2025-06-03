package com.coincommunity.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 암호화폐 뉴스 정보를 저장하는 엔티티
 */
@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(nullable = false)
    private String source;
    
    private String author;
    
    @Column(nullable = false)
    private String url;
    
    private String imageUrl;
    
    @Column(nullable = false)
    private LocalDateTime publishedAt;
    
    @Builder.Default
    private Integer viewCount = 0;
    
    /**
     * 조회수를 증가시킵니다.
     */
    public void increaseViewCount() {
        this.viewCount += 1;
    }
}
