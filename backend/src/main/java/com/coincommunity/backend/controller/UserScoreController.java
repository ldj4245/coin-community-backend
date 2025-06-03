package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자 점수 관리 컨트롤러
 * 사용자의 활동 점수 및 레벨을 관리합니다.
 */
@RestController
@RequestMapping("/api/user-score")
@RequiredArgsConstructor
@Slf4j
public class UserScoreController {
    
    /**
     * 사용자 점수 조회
     */
    @GetMapping("/{userId}")
    @Operation(summary = "사용자 점수 조회", description = "특정 사용자의 점수와 레벨 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserScore(@PathVariable Long userId) {
        log.info("사용자 점수 조회 요청: userId={}", userId);
        
        try {
            // TODO: 실제 점수 조회 로직 구현
            Map<String, Object> scoreInfo = Map.of(
                "userId", userId,
                "totalScore", 1250,
                "level", 5,
                "nextLevelScore", 1500,
                "rank", 127
            );
            
            return ResponseEntity.ok(ApiResponse.success(scoreInfo));
        } catch (Exception e) {
            log.error("사용자 점수 조회 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("점수 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 점수 랭킹 조회
     */
    @GetMapping("/ranking")
    @Operation(summary = "점수 랭킹 조회", description = "전체 사용자 점수 랭킹을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getScoreRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("점수 랭킹 조회 요청: page={}, size={}", page, size);
        
        try {
            // TODO: 실제 랭킹 조회 로직 구현
            return ResponseEntity.ok(ApiResponse.success("랭킹 조회 기능 구현 예정"));
        } catch (Exception e) {
            log.error("점수 랭킹 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("랭킹 조회 중 오류가 발생했습니다."));
        }
    }
}