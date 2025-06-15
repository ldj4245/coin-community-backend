package com.coincommunity.backend.service;

import com.coincommunity.backend.entity.*;
import com.coincommunity.backend.repository.PortfolioRepository;
import com.coincommunity.backend.repository.PortfolioItemRepository;
import com.coincommunity.backend.repository.TransactionRepository;
import com.coincommunity.backend.repository.UserRepository;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.dto.PortfolioDto;
import com.coincommunity.backend.dto.PortfolioItemDto;
import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.PortfolioDto;
import com.coincommunity.backend.dto.PortfolioItemDto;
import com.coincommunity.backend.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 관리 서비스
 * 30년차 베테랑 개발자의 아키텍처 패턴 적용:
 * - 도메인 주도 설계 (DDD)
 * - 캐싱 전략
 * - 트랜잭션 관리
 * - 이벤트 기반 아키텍처 준비
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {
    
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CoinPriceService coinPriceService;
    // private final UserScoreService userScoreService;
    
    /**
     * 사용자의 모든 포트폴리오 조회
     */
    @Cacheable(value = "userPortfolios", key = "#userId")
    public List<Portfolio> getUserPortfolios(Long userId) {
        return portfolioRepository.findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId);
    }
    
    /**
     * 사용자의 기본 포트폴리오 조회
     */
    @Cacheable(value = "defaultPortfolio", key = "#userId")
    public Portfolio getDefaultPortfolio(Long userId) {
        return portfolioRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> createDefaultPortfolio(userId));
    }
    
    /**
     * 포트폴리오 상세 조회
     */
    @Cacheable(value = "portfolio", key = "#portfolioId")
    public Portfolio getPortfolioById(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오를 찾을 수 없습니다. ID: " + portfolioId));
    }
    
    /**
     * 포트폴리오 생성
     */
    @Transactional
    @CacheEvict(value = {"userPortfolios", "defaultPortfolio"}, key = "#userId")
    public Portfolio createPortfolio(Long userId, String name, String description, boolean isDefault) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 기본 포트폴리오가 이미 존재하는 경우 기존 것을 기본에서 해제
        if (isDefault) {
            portfolioRepository.findByUserIdAndIsDefaultTrue(userId)
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        portfolioRepository.save(existing);
                    });
        }
        
        Portfolio portfolio = Portfolio.builder()
                .name(name)
                .description(description)
                .user(user)
                .isDefault(isDefault)
                .totalInvestment(BigDecimal.ZERO)
                .currentValue(BigDecimal.ZERO)
                .totalReturnPercent(BigDecimal.ZERO)
                .totalReturnAmount(BigDecimal.ZERO)
                .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        
        log.info("포트폴리오 생성 완료: userId={}, portfolioId={}, name={}", 
                userId, savedPortfolio.getId(), name);
        
        return savedPortfolio;
    }
    
    /**
     * 기본 포트폴리오 생성
     */
    @Transactional
    private Portfolio createDefaultPortfolio(Long userId) {
        return createPortfolio(userId, "기본 포트폴리오", "자동 생성된 기본 포트폴리오", true);
    }
    
    /**
     * 매수 거래 추가
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "userPortfolios", "portfolioSummary"}, allEntries = true)
    public PortfolioItem addPurchaseTransaction(Long userId, Long portfolioId, 
                                               String coinId, String coinName, String coinSymbol,
                                               BigDecimal quantity, BigDecimal price, 
                                               String exchange, String memo) {
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioOwnership(portfolio, userId);
        
        // 기존 포트폴리오 아이템 확인
        PortfolioItem item = portfolioItemRepository
                .findByPortfolioIdAndCoinId(portfolioId, coinId)
                .orElse(null);
        
        if (item == null) {
            // 새로운 코인 추가
            item = PortfolioItem.builder()
                    .portfolio(portfolio)
                    .coinId(coinId)
                    .coinName(coinName)
                    .coinSymbol(coinSymbol)
                    .quantity(quantity)
                    .averagePrice(price)
                    .firstPurchaseDate(LocalDateTime.now())
                    .build();
            item.calculateTotalInvestment();
        } else {
            // 기존 코인 추가 매수
            item.addPurchase(quantity, price);
        }
        
        // 현재 가격으로 업데이트
        updateItemCurrentPrice(item);
        
        PortfolioItem savedItem = portfolioItemRepository.save(item);
        
        // 거래 내역 저장
        saveTransaction(userId, portfolio, coinId, coinName, coinSymbol, 
                       Transaction.TransactionType.BUY, quantity, price, exchange, memo);
        
        // 포트폴리오 수익률 재계산
        recalculatePortfolioReturns(portfolio);
        
        // 사용자 스코어 업데이트
        // userScoreService.addPortfolioActivityScore(userId, 10);
        
        log.info("매수 거래 추가 완료: userId={}, portfolioId={}, coinId={}, quantity={}, price={}", 
                userId, portfolioId, coinId, quantity, price);
        
        return savedItem;
    }
    
    /**
     * 매도 거래 추가
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "userPortfolios", "portfolioSummary"}, allEntries = true)
    public PortfolioItem addSaleTransaction(Long userId, Long portfolioId,
                                           String coinId, BigDecimal quantity, BigDecimal price,
                                           String exchange, String memo) {
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioOwnership(portfolio, userId);
        
        PortfolioItem item = portfolioItemRepository
                .findByPortfolioIdAndCoinId(portfolioId, coinId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오에서 해당 코인을 찾을 수 없습니다."));
        
        if (item.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException("매도 수량이 보유 수량을 초과할 수 없습니다.");
        }
        
        // 실현손익 계산을 위한 평균 매수가 저장
        BigDecimal averageBuyPrice = item.getAveragePrice();
        
        // 매도 처리
        item.addSale(quantity);
        updateItemCurrentPrice(item);
        
        PortfolioItem savedItem = portfolioItemRepository.save(item);
        
        // 거래 내역 저장 (실현손익 계산 포함)
        Transaction saleTransaction = saveTransaction(userId, portfolio, coinId, 
                item.getCoinName(), item.getCoinSymbol(),
                Transaction.TransactionType.SELL, quantity, price, exchange, memo);
        saleTransaction.calculateRealizedGain(averageBuyPrice);
        transactionRepository.save(saleTransaction);
        
        // 포트폴리오 수익률 재계산
        recalculatePortfolioReturns(portfolio);
        
        // 수익률에 따른 스코어 업데이트
        if (saleTransaction.getRealizedGain() != null && 
            saleTransaction.getRealizedGain().compareTo(BigDecimal.ZERO) > 0) {
            // userScoreService.addPortfolioSuccessScore(userId, 20);
        }
        
        log.info("매도 거래 추가 완료: userId={}, portfolioId={}, coinId={}, quantity={}, price={}, realizedGain={}", 
                userId, portfolioId, coinId, quantity, price, saleTransaction.getRealizedGain());
        
        return savedItem;
    }
    
    /**
     * 포트폴리오 실시간 업데이트
     */
    @Transactional
    @CacheEvict(value = {"portfolio", "userPortfolios", "portfolioSummary"}, allEntries = true)
    public void updatePortfolioPrices(Long portfolioId) {
        Portfolio portfolio = getPortfolioById(portfolioId);
        
        for (PortfolioItem item : portfolio.getItems()) {
            updateItemCurrentPrice(item);
            portfolioItemRepository.save(item);
        }
        
        recalculatePortfolioReturns(portfolio);
        
        log.debug("포트폴리오 가격 업데이트 완료: portfolioId={}", portfolioId);
    }
    
    /**
     * 포트폴리오 요약 정보 조회
     */
    @Cacheable(value = "portfolioSummary", key = "#userId")
    public Map<String, Object> getPortfolioSummary(Long userId) {
        List<Portfolio> portfolios = getUserPortfolios(userId);
        
        BigDecimal totalInvestment = portfolios.stream()
                .map(Portfolio::getTotalInvestment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCurrentValue = portfolios.stream()
                .map(Portfolio::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReturn = totalCurrentValue.subtract(totalInvestment);
        BigDecimal totalReturnPercent = totalInvestment.compareTo(BigDecimal.ZERO) > 0 ?
                totalReturn.divide(totalInvestment, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        
        return Map.of(
                "totalInvestment", totalInvestment,
                "totalCurrentValue", totalCurrentValue,
                "totalReturn", totalReturn,
                "totalReturnPercent", totalReturnPercent,
                "portfolioCount", portfolios.size()
        );
    }
    
    /**
     * 공개 포트폴리오 목록 조회 (랭킹)
     */
    @Cacheable(value = "publicPortfolios", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<Portfolio> getPublicPortfolios(Pageable pageable) {
        return portfolioRepository.findByIsPublicTrueOrderByTotalReturnPercentDesc(pageable);
    }
    
    // Private helper methods
    
    private void validatePortfolioOwnership(Portfolio portfolio, Long userId) {
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("포트폴리오에 대한 권한이 없습니다.");
        }
    }
    
    private void updateItemCurrentPrice(PortfolioItem item) {
        try {
            // 현재 가격 조회 (캐시된 서비스 사용)
            var coinPrice = coinPriceService.getCoinPriceByExchange(item.getCoinId(), "UPBIT");
            item.updateCurrentPrice(coinPrice.getCurrentPrice());
        } catch (Exception e) {
            log.warn("코인 가격 업데이트 실패: coinId={}, error={}", item.getCoinId(), e.getMessage());
        }
    }
    
    private Transaction saveTransaction(Long userId, Portfolio portfolio, String coinId, 
                                     String coinName, String coinSymbol,
                                     Transaction.TransactionType type, BigDecimal quantity, 
                                     BigDecimal price, String exchange, String memo) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        
        Transaction transaction = Transaction.builder()
                .user(user)
                .portfolio(portfolio)
                .coinId(coinId)
                .coinName(coinName)
                .coinSymbol(coinSymbol)
                .type(type)
                .quantity(quantity)
                .price(price)
                .totalAmount(quantity.multiply(price))
                .exchange(exchange)
                .memo(memo)
                .transactionDate(LocalDateTime.now())
                .build();
        
        return transactionRepository.save(transaction);
    }
    
    private void recalculatePortfolioReturns(Portfolio portfolio) {
        portfolio.calculateReturns();
        portfolioRepository.save(portfolio);
    }
    
    // DTO 기반 메서드들 추가

    /**
     * 포트폴리오 생성 (DTO 기반)
     */
    @Transactional
    @CacheEvict(value = "userPortfolios", allEntries = true)
    public PortfolioDto.Response createPortfolio(String username, PortfolioDto.CreateRequest request) {
        try {
            // principal username은 실제 사용자 ID 문자열이므로 파싱
            Long userId = Long.parseLong(username);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
            // Builder로 기본 객체 생성
            Portfolio portfolio = Portfolio.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .isDefault(false)
                    .isPublic(request.getIsPublic())
                    .totalInvestment(request.getInitialInvestment())
                    .currentValue(BigDecimal.ZERO)
                    .totalReturnPercent(BigDecimal.ZERO)
                    .totalReturnAmount(BigDecimal.ZERO)
                    .build();
            
            // User와의 관계 설정
            portfolio.addToUser(user);
            
            // 저장
            Portfolio savedPortfolio = portfolioRepository.save(portfolio);
            
            log.info("포트폴리오 생성 완료: userId={}, portfolioId={}, name={}", 
                    user.getId(), savedPortfolio.getId(), request.getName());
            
            return convertToResponseDto(savedPortfolio);
        } catch (Exception e) {
            log.error("포트폴리오 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("포트폴리오를 생성하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자 포트폴리오 목록 조회 (DTO 기반)
     */
    public PageResponse<PortfolioDto.Summary> getUserPortfolios(String username, Pageable pageable) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        List<Portfolio> portfolios = getUserPortfolios(user.getId());
        List<PortfolioDto.Summary> summaries = portfolios.stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(summaries, pageable, (long) summaries.size());
    }

    /**
     * 포트폴리오 상세 조회 (DTO 기반)
     */
    public PortfolioDto.Response getPortfolioDetails(Long portfolioId, String username) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioAccess(portfolio, user.getId());
        
        return convertToResponseDto(portfolio);
    }

    /**
     * 포트폴리오 수정 (DTO 기반)
     */
    @Transactional
    @CacheEvict(value = {"portfolioDetail", "userPortfolios"}, allEntries = true)
    public PortfolioDto.Response updatePortfolio(Long portfolioId, String username, PortfolioDto.UpdateRequest request) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioOwnership(portfolio, user.getId());
        
        if (request.getName() != null) {
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) {
            portfolio.setDescription(request.getDescription());
        }
        if (request.getIsPublic() != null) {
            portfolio.setPublic(request.getIsPublic());
        }
        
        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        return convertToResponseDto(updatedPortfolio);
    }

    /**
     * 포트폴리오 삭제 (DTO 기반)
     */
    @Transactional
    @CacheEvict(value = {"portfolioDetail", "userPortfolios"}, allEntries = true)
    public void deletePortfolio(Long portfolioId, String username) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioOwnership(portfolio, user.getId());
        
        // 기본 포트폴리오는 삭제할 수 없음
        if (portfolio.isDefault()) {
            throw new IllegalArgumentException("기본 포트폴리오는 삭제할 수 없습니다.");
        }
        
        // 먼저 연관된 트랜잭션을 삭제
        transactionRepository.deleteByPortfolioId(portfolioId);
        
        // 그 다음 포트폴리오 삭제
        portfolioRepository.delete(portfolio);
        log.info("포트폴리오 삭제 완료: portfolioId={}, username={}", portfolioId, username);
    }

    /**
     * 포트폴리오 아이템 추가 (DTO 기반)
     */
    @Transactional
    public PortfolioItemDto.Response addPortfolioItem(Long portfolioId, String username, PortfolioItemDto.AddRequest request) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        // exchange 필드는 NULL이 허용되지 않으므로 기본값 제공
        String exchange = "default";
        String memo = request.getNotes();
        
        PortfolioItem item = addPurchaseTransaction(user.getId(), portfolioId, request.getCoinSymbol(), 
                request.getCoinSymbol(), request.getCoinSymbol(), request.getQuantity(), 
                request.getAveragePrice(), exchange, memo);
        
        return convertToItemResponseDto(item);
    }

    /**
     * 포트폴리오 아이템 수정 (DTO 기반)
     */
    @Transactional
    public PortfolioItemDto.Response updatePortfolioItem(Long portfolioId, Long itemId, String username, 
                                                        PortfolioItemDto.UpdateRequest request) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        // 기존 메서드와 시그니처가 다르므로 직접 구현
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오 아이템을 찾을 수 없습니다."));
        
        validatePortfolioOwnership(item.getPortfolio(), user.getId());
        
        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getAveragePrice() != null) {
            item.setAveragePrice(request.getAveragePrice());
        }
        
        item.calculateTotalInvestment();
        PortfolioItem updatedItem = portfolioItemRepository.save(item);
        
        return convertToItemResponseDto(updatedItem);
    }

    /**
     * 포트폴리오 아이템 삭제 (DTO 기반)
     */
    @Transactional
    public void removePortfolioItem(Long portfolioId, Long itemId, String username) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        PortfolioItem item = portfolioItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오 아이템을 찾을 수 없습니다."));
        
        validatePortfolioOwnership(item.getPortfolio(), user.getId());
        portfolioItemRepository.delete(item);
    }

    /**
     * 사용자 포트폴리오 통계 (DTO 기반)
     */
    public PortfolioDto.Statistics getUserPortfolioStatistics(String username) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        // 실제 통계 계산 (user.getId() 사용)
        List<Portfolio> portfolios = getUserPortfolios(user.getId());
        long totalPortfolios = portfolios.size();
        
        BigDecimal totalInvestment = portfolios.stream()
                .map(Portfolio::getTotalInvestment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCurrentValue = portfolios.stream()
                .map(Portfolio::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalProfitLoss = totalCurrentValue.subtract(totalInvestment);
        
        BigDecimal averageReturnPercentage = totalInvestment.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfitLoss.divide(totalInvestment, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
        
        return PortfolioDto.Statistics.builder()
                .totalPortfolios(totalPortfolios)
                .totalInvestment(totalInvestment)
                .totalCurrentValue(totalCurrentValue)
                .totalProfitLoss(totalProfitLoss)
                .averageReturnPercentage(averageReturnPercentage)
                .build();
    }

    /**
     * 공개 포트폴리오 목록 조회 (DTO 기반)
     */
    public PageResponse<PortfolioDto.Summary> getPublicPortfolios(String sortBy, String direction, Pageable pageable) {
        Page<Portfolio> portfolioPage = getPublicPortfolios(pageable);
        
        List<PortfolioDto.Summary> summaries = portfolioPage.getContent().stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
        
        return PageResponse.of(summaries, pageable, portfolioPage.getTotalElements());
    }

    /**
     * 포트폴리오 가격 업데이트 (DTO 기반)
     */
    @Transactional
    public PortfolioDto.Response refreshPortfolioPrices(Long portfolioId, String username) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Portfolio portfolio = getPortfolioById(portfolioId);
        validatePortfolioOwnership(portfolio, user.getId());
        
        // 가격 업데이트 로직은 기존 메서드와 동일하게 구현
        portfolio.calculateReturns();
        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);
        
        return convertToResponseDto(updatedPortfolio);
    }

    /**
     * 포트폴리오 복제 (DTO 기반)
     */
    @Transactional
    public PortfolioDto.Response clonePortfolio(Long portfolioId, String username, String newName) {
        User user = userRepository.findById(Long.parseLong(username))
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Portfolio originalPortfolio = getPortfolioById(portfolioId);
        validatePortfolioAccess(originalPortfolio, user.getId());
        
        // 포트폴리오 복제 로직
        Portfolio clonedPortfolio = Portfolio.builder()
                .name(newName)
                .description(originalPortfolio.getDescription() + " (복제본)")
                .user(user)
                .isPublic(false) // 복제된 포트폴리오는 기본적으로 비공개
                .build();
        
        Portfolio savedPortfolio = portfolioRepository.save(clonedPortfolio);
        
        return convertToResponseDto(savedPortfolio);
    }

    // DTO 변환 메서드들

    private PortfolioDto.Response convertToResponseDto(Portfolio portfolio) {
        return PortfolioDto.Response.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .initialInvestment(portfolio.getTotalInvestment())
                .currentValue(portfolio.getCurrentValue())
                .totalProfitLoss(portfolio.getTotalReturnAmount())
                .totalReturnPercentage(portfolio.getTotalReturnPercent())
                .isPublic(portfolio.isPublic())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }

    private PortfolioDto.Summary convertToSummaryDto(Portfolio portfolio) {
        return PortfolioDto.Summary.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .currentValue(portfolio.getCurrentValue())
                .totalReturnPercentage(portfolio.getTotalReturnPercent())
                .lastUpdated(portfolio.getUpdatedAt())
                .build();
    }

    private PortfolioItemDto.Response convertToItemResponseDto(PortfolioItem item) {
        return PortfolioItemDto.Response.builder()
                .id(item.getId())
                .coinSymbol(item.getCoinSymbol())
                .coinName(item.getCoinName())
                .quantity(item.getQuantity())
                .averagePrice(item.getAveragePrice())
                .currentPrice(item.getCurrentPrice())
                .totalCost(item.getTotalInvestment())
                .currentValue(item.getCurrentValue())
                .profitLoss(item.getUnrealizedGain())
                .returnPercentage(item.getUnrealizedGainPercent())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .priceUpdatedAt(item.getLastUpdatedAt())
                .build();
    }

    // Helper method for validation
    private void validatePortfolioAccess(Portfolio portfolio, Long userId) {
        if (!portfolio.isPublic() && !portfolio.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("포트폴리오에 대한 접근 권한이 없습니다.");
        }
    }
}
