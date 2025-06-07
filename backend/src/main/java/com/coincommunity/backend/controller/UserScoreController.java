package com.coincommunity.backend.controller;

import com.coincommunity.backend.dto.ApiResponse;
import com.coincommunity.backend.service.UserScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    
    private final UserScoreService userScoreService;
    
    /**
     * 내 점수 조회
     */
    @GetMapping("/my")
    @Operation(summary = "내 점수 조회", description = "로그인한 사용자의 점수와 레벨 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getMyScore(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("내 점수 조회 요청: 사용자={}", userDetails.getUsername());
        
        try {
            Object scoreInfo = userScoreService.getUserScore(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(scoreInfo));
        } catch (Exception e) {
            log.error("내 점수 조회 중 오류 발생: 사용자={}", userDetails.getUsername(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("점수 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 상위 사용자 조회
     */
    @GetMapping("/top")
    @Operation(summary = "상위 사용자 조회", description = "점수 상위 사용자 목록을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getTopUsers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("상위 사용자 조회 요청: limit={}", limit);
        
        try {
            Object topUsers = userScoreService.getTopUsers(limit);
            return ResponseEntity.ok(ApiResponse.success(topUsers));
        } catch (Exception e) {
            log.error("상위 사용자 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("상위 사용자 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 레벨 시스템 정보 조회
     */
    @GetMapping("/levels")
    @Operation(summary = "레벨 시스템 정보", description = "레벨 시스템 정보를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getLevelSystem() {
        log.info("레벨 시스템 정보 조회 요청");
        
        try {
            Object levelSystem = userScoreService.getLevelSystem();
            return ResponseEntity.ok(ApiResponse.success(levelSystem));
        } catch (Exception e) {
            log.error("레벨 시스템 정보 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("레벨 시스템 정보 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 내 성취도 조회
     */
    @GetMapping("/my/achievements")
    @Operation(summary = "내 성취도 조회", description = "로그인한 사용자의 성취도를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Object>> getMyAchievements(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("내 성취도 조회 요청: 사용자={}", userDetails.getUsername());
        
        try {
            Object achievements = userScoreService.getUserAchievements(userDetails.getUsername());
            return ResponseEntity.ok(ApiResponse.success(achievements));
        } catch (Exception e) {
            log.error("내 성취도 조회 중 오류 발생: 사용자={}", userDetails.getUsername(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("성취도 조회 중 오류가 발생했습니다."));
        }
    }
}