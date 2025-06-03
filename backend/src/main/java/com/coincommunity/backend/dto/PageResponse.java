package com.coincommunity.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 페이지 응답 DTO
 * Spring Data의 Page 인터페이스를 클라이언트에 전달하기 위한 DTO 클래스
 * 
 * @param <T> 페이지 내용의 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이지 응답")
public class PageResponse<T> {

    @Schema(description = "페이지 내용")
    private List<T> content;

    @Schema(description = "총 항목 수", example = "150")
    private Long totalElements;

    @Schema(description = "총 페이지 수", example = "15")
    private Integer totalPages;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private Integer currentPage;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;

    @Schema(description = "첫 페이지 여부", example = "true")
    private Boolean first;

    @Schema(description = "마지막 페이지 여부", example = "false")
    private Boolean last;

    @Schema(description = "비어있는지 여부", example = "false")
    private Boolean empty;

    /**
     * Spring Data Page 객체로부터 PageResponse 객체 생성
     * 
     * @param <T> 페이지 내용의 타입
     * @param page Spring Data Page 객체
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    /**
     * Spring Data Page 객체로부터 PageResponse 객체 생성 (of 메소드의 별칭)
     * 
     * @param <T> 페이지 내용의 타입
     * @param page Spring Data Page 객체
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return of(page);
    }

    /**
     * 리스트와 페이지 정보로부터 PageResponse 객체 생성
     * 
     * @param <T> 페이지 내용의 타입
     * @param content 페이지 내용
     * @param pageable 페이지 정보
     * @param totalElements 총 항목 수
     * @return PageResponse 객체
     */
    public static <T> PageResponse<T> of(List<T> content, Pageable pageable, Long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        
        return PageResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .first(pageable.getPageNumber() == 0)
                .last(pageable.getPageNumber() >= totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }

    /**
     * 빈 PageResponse 객체 생성
     * 
     * @param <T> 페이지 내용의 타입
     * @param pageable 페이지 정보
     * @return 빈 PageResponse 객체
     */
    public static <T> PageResponse<T> empty(Pageable pageable) {
        return PageResponse.<T>builder()
                .content(List.of())
                .totalElements(0L)
                .totalPages(0)
                .currentPage(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }

    /**
     * Spring Data Page 객체와 변환된 컨텐츠로부터 PageResponse 객체 생성
     * 
     * @param <S> 원본 페이지 내용의 타입
     * @param <T> 변환된 페이지 내용의 타입
     * @param page Spring Data Page 객체
     * @param convertedContent 변환된 페이지 내용
     * @return PageResponse 객체
     */
    public static <S, T> PageResponse<T> from(Page<S> page, List<T> convertedContent) {
        return PageResponse.<T>builder()
                .content(convertedContent)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(convertedContent.isEmpty())
                .build();
    }
}
