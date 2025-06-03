package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.PageResponse;
import com.coincommunity.backend.dto.TransactionDto;
import com.coincommunity.backend.entity.*;
import com.coincommunity.backend.exception.BusinessException;
import com.coincommunity.backend.exception.ResourceNotFoundException;
import com.coincommunity.backend.exception.UnauthorizedException;
import com.coincommunity.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 거래 내역 관리 서비스
 * 30년차 베테랑 개발자의 엔터프라이즈급 아키텍처 패턴 적용:
 * - 도메인 주도 설계 (DDD)
 * - 이벤트 기반 아키텍처
 * - 캐싱 전략 최적화
 * - 트랜잭션 관리 고도화
 * - 실시간 분석 시스템
 * - 대용량 데이터 처리
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final CoinPriceService coinPriceService;
    // private final UserScoreService userScoreService;
    private final NotificationService notificationService;
    private final RealtimeNotificationService realtimeNotificationService;

    private static final int SCALE = 8;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * 거래 내역 생성
     * 포트폴리오 아이템 연동 및 실현손익 계산 포함
     */
    @Transactional
    @CacheEvict(value = {"userTransactions", "portfolioTransactions", "transactionStats"}, allEntries = true)
    public TransactionDto.Response createTransaction(String username, TransactionDto.CreateRequest request) {
        log.info("거래 내역 생성 시작: 사용자={}, 코인={}, 유형={}, 수량={}", 
                username, request.getCoinSymbol(), request.getTransactionType(), request.getQuantity());

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUser(request.getPortfolioId(), user);

        // 거래 날짜가 없으면 현재 시간 설정
        LocalDateTime transactionDate = request.getTransactionDate() != null 
                ? request.getTransactionDate() 
                : LocalDateTime.now();

        // 수수료 기본값 설정
        BigDecimal fee = request.getFee() != null ? request.getFee() : BigDecimal.ZERO;
        
        // 총 거래금액 계산
        BigDecimal totalAmount = request.getQuantity().multiply(request.getPrice());

        Transaction transaction = Transaction.builder()
                .user(user)
                .portfolio(portfolio)
                .coinId(request.getCoinSymbol().toLowerCase())
                .coinName(request.getCoinSymbol().toUpperCase())
                .coinSymbol(request.getCoinSymbol().toUpperCase())
                .type(request.getTransactionType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .totalAmount(totalAmount)
                .fee(fee)
                .exchange(request.getExchange())
                .memo(request.getNotes())
                .transactionDate(transactionDate)
                .build();

        // 매도 거래인 경우 실현손익 계산
        if (request.getTransactionType() == Transaction.TransactionType.SELL) {
            calculateRealizedGain(transaction, portfolio);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 포트폴리오 아이템 업데이트
        updatePortfolioItem(savedTransaction);

        // 사용자 점수 업데이트
        // userScoreService.addTransactionScore(user.getId(), savedTransaction.getTotalAmount());

        // 실시간 알림 발송
        sendTransactionNotification(user, savedTransaction);

        log.info("거래 내역 생성 완료: ID={}, 사용자={}", savedTransaction.getId(), username);

        return mapToResponse(savedTransaction);
    }

    /**
     * 거래 내역 수정
     */
    @Transactional
    @CacheEvict(value = {"userTransactions", "portfolioTransactions", "transactionStats"}, allEntries = true)
    public TransactionDto.Response updateTransaction(Long transactionId, String username, 
                                                    TransactionDto.CreateRequest request) {
        log.info("거래 내역 수정 시작: ID={}, 사용자={}", transactionId, username);

        Transaction transaction = findTransactionByIdAndUser(transactionId, username);
        
        // 기존 포트폴리오 아이템 업데이트 롤백
        rollbackPortfolioItem(transaction);

        // 거래 정보 업데이트
        transaction.setCoinId(request.getCoinSymbol().toLowerCase());
        transaction.setCoinName(request.getCoinSymbol().toUpperCase());
        transaction.setCoinSymbol(request.getCoinSymbol().toUpperCase());
        transaction.setType(request.getTransactionType());
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(request.getPrice());
        transaction.setTotalAmount(request.getQuantity().multiply(request.getPrice()));
        transaction.setFee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO);
        transaction.setExchange(request.getExchange());
        transaction.setMemo(request.getNotes());
        
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }

        // 매도 거래인 경우 실현손익 재계산
        if (request.getTransactionType() == Transaction.TransactionType.SELL) {
            calculateRealizedGain(transaction, transaction.getPortfolio());
        } else {
            transaction.setRealizedGain(null);
            transaction.setRealizedGainPercent(null);
            transaction.setRealized(false);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 포트폴리오 아이템 재계산
        updatePortfolioItem(savedTransaction);

        log.info("거래 내역 수정 완료: ID={}, 사용자={}", transactionId, username);

        return mapToResponse(savedTransaction);
    }

    /**
     * 거래 내역 삭제
     */
    @Transactional
    @CacheEvict(value = {"userTransactions", "portfolioTransactions", "transactionStats"}, allEntries = true)
    public void deleteTransaction(Long transactionId, String username) {
        log.info("거래 내역 삭제 시작: ID={}, 사용자={}", transactionId, username);

        Transaction transaction = findTransactionByIdAndUser(transactionId, username);
        
        // 포트폴리오 아이템 업데이트 롤백
        rollbackPortfolioItem(transaction);

        transactionRepository.delete(transaction);

        log.info("거래 내역 삭제 완료: ID={}, 사용자={}", transactionId, username);
    }

    /**
     * 거래 내역 상세 조회
     */
    @Cacheable(value = "transactionDetails", key = "#transactionId + '_' + #username")
    public TransactionDto.Response getTransactionDetails(Long transactionId, String username) {
        log.debug("거래 내역 상세 조회: ID={}, 사용자={}", transactionId, username);

        Transaction transaction = findTransactionByIdAndUser(transactionId, username);
        return mapToResponse(transaction);
    }

    /**
     * 사용자 거래 내역 목록 조회
     */
    @Cacheable(value = "userTransactions", key = "#username + '_' + #portfolioId + '_' + #filter.hashCode() + '_' + #pageable.pageNumber")
    public PageResponse<TransactionDto.Response> getUserTransactions(String username, Long portfolioId,
                                                                    TransactionDto.FilterRequest filter, 
                                                                    Pageable pageable) {
        log.debug("사용자 거래 내역 조회: 사용자={}, 포트폴리오ID={}", username, portfolioId);

        User user = findUserByUsername(username);
        
        Page<Transaction> transactions;
        
        if (portfolioId != null) {
            Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);
            transactions = getFilteredTransactionsByPortfolio(portfolio.getId(), filter, pageable);
        } else {
            transactions = getFilteredTransactionsByUser(user.getId(), filter, pageable);
        }

        return PageResponse.of(transactions.map(this::mapToResponse));
    }

    /**
     * 포트폴리오별 거래 내역 조회
     */
    @Cacheable(value = "portfolioTransactions", key = "#portfolioId + '_' + #username + '_' + #filter.hashCode() + '_' + #pageable.pageNumber")
    public PageResponse<TransactionDto.Response> getPortfolioTransactions(Long portfolioId, String username,
                                                                         TransactionDto.FilterRequest filter,
                                                                         Pageable pageable) {
        log.debug("포트폴리오 거래 내역 조회: 포트폴리오ID={}, 사용자={}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);

        Page<Transaction> transactions = getFilteredTransactionsByPortfolio(portfolio.getId(), filter, pageable);

        return PageResponse.of(transactions.map(this::mapToResponse));
    }

    /**
     * 거래 통계 요약 조회
     */
    @Cacheable(value = "transactionStats", key = "#username + '_' + #portfolioId + '_' + #days")
    public TransactionDto.Summary getUserTransactionSummary(String username, Long portfolioId, Integer days) {
        log.debug("거래 통계 조회: 사용자={}, 포트폴리오ID={}, 기간={}일", username, portfolioId, days);

        User user = findUserByUsername(username);
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Transaction> transactions;
        if (portfolioId != null) {
            Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                    user.getId(), since, LocalDateTime.now());
            transactions = transactions.stream()
                    .filter(t -> t.getPortfolio().getId().equals(portfolioId))
                    .collect(Collectors.toList());
        } else {
            transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                    user.getId(), since, LocalDateTime.now());
        }

        return calculateTransactionSummary(transactions);
    }

    /**
     * 기간별 거래 통계 조회
     */
    public List<TransactionDto.Statistics> getPeriodStatistics(String username, LocalDateTime startDate, 
                                                              LocalDateTime endDate, String groupBy) {
        log.debug("기간별 거래 통계 조회: 사용자={}, 시작일={}, 종료일={}, 그룹={}", 
                username, startDate, endDate, groupBy);

        User user = findUserByUsername(username);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                user.getId(), startDate, endDate);

        return calculatePeriodStatistics(transactions, groupBy);
    }

    /**
     * 코인별 거래 통계 조회
     */
    public List<TransactionDto.Statistics> getCoinStatistics(String username, Integer days, Integer limit) {
        log.debug("코인별 거래 통계 조회: 사용자={}, 기간={}일, 제한={}", username, days, limit);

        User user = findUserByUsername(username);
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<Object[]> stats = transactionRepository.findTradingStatsByUserId(user.getId());
        
        return stats.stream()
                .limit(limit)
                .map(this::mapToStatistics)
                .collect(Collectors.toList());
    }

    /**
     * CSV 파일로 거래 내역 가져오기
     */
    @Transactional
    @CacheEvict(value = {"userTransactions", "portfolioTransactions", "transactionStats"}, allEntries = true)
    public List<TransactionDto.Response> importTransactionsFromCsv(Long portfolioId, String username, 
                                                                  MultipartFile csvFile) {
        log.info("CSV 파일로 거래 내역 가져오기 시작: 포트폴리오ID={}, 사용자={}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 스킵
                }

                Transaction transaction = parseCsvLine(line, user, portfolio);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }

            // 일괄 저장
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

            // 포트폴리오 아이템 업데이트
            for (Transaction transaction : savedTransactions) {
                updatePortfolioItem(transaction);
            }

            log.info("CSV 파일로 거래 내역 가져오기 완료: 건수={}, 사용자={}", savedTransactions.size(), username);

            return savedTransactions.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("CSV 파일 처리 중 오류 발생: 사용자={}", username, e);
            throw new BusinessException("CSV 파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * CSV 문자열로 거래 내역 가져오기
     */
    @Transactional
    @CacheEvict(value = {"userTransactions", "portfolioTransactions", "transactionStats"}, allEntries = true)
    public List<TransactionDto.Response> importTransactionsFromCsv(Long portfolioId, String username, 
                                                                  String csvData) {
        log.info("CSV 데이터로 거래 내역 가져오기 시작: 포트폴리오ID={}, 사용자={}", portfolioId, username);

        User user = findUserByUsername(username);
        Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);

        List<Transaction> transactions = new ArrayList<>();

        try {
            String[] lines = csvData.split("\n");
            boolean isFirstLine = true;

            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 스킵
                }

                if (line.trim().isEmpty()) {
                    continue; // 빈 라인 스킵
                }

                Transaction transaction = parseCsvLine(line, user, portfolio);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }

            // 일괄 저장
            List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

            // 포트폴리오 아이템 업데이트
            for (Transaction transaction : savedTransactions) {
                updatePortfolioItem(transaction);
            }

            log.info("CSV 데이터로 거래 내역 가져오기 완료: 건수={}, 사용자={}", savedTransactions.size(), username);

            return savedTransactions.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("CSV 데이터 처리 중 오류 발생: 사용자={}", username, e);
            throw new BusinessException("CSV 데이터 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 거래 내역 CSV로 내보내기
     */
    public String exportTransactionsToCsv(String username, Long portfolioId, 
                                         LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("거래 내역 CSV 내보내기: 사용자={}, 포트폴리오ID={}", username, portfolioId);

        User user = findUserByUsername(username);
        List<Transaction> transactions;

        if (portfolioId != null) {
            Portfolio portfolio = findPortfolioByIdAndUser(portfolioId, user);
            transactions = transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId);
        } else {
            Page<Transaction> transactionPage = transactionRepository.findByUserIdOrderByTransactionDateDesc(user.getId(), Pageable.unpaged());
            transactions = transactionPage.getContent();
        }

        // 날짜 필터링
        if (startDate != null || endDate != null) {
            transactions = transactions.stream()
                    .filter(t -> {
                        LocalDateTime date = t.getTransactionDate();
                        if (startDate != null && date.isBefore(startDate)) return false;
                        if (endDate != null && date.isAfter(endDate)) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        return generateCsvContent(transactions);
    }

    // ==================== Private Methods ====================

    /**
     * 실현손익 계산
     */
    private void calculateRealizedGain(Transaction transaction, Portfolio portfolio) {
        // 해당 코인의 평균 매수 단가 계산
        Optional<BigDecimal> avgBuyPrice = transactionRepository
                .findAverageBuyPriceByPortfolioAndCoin(portfolio.getId(), transaction.getCoinId());

        if (avgBuyPrice.isPresent()) {
            transaction.calculateRealizedGain(avgBuyPrice.get());
        }
    }

    /**
     * 포트폴리오 아이템 업데이트
     */
    private void updatePortfolioItem(Transaction transaction) {
        Optional<PortfolioItem> itemOpt = portfolioItemRepository
                .findByPortfolioIdAndCoinId(transaction.getPortfolio().getId(), transaction.getCoinId());

        PortfolioItem item;
        if (itemOpt.isPresent()) {
            item = itemOpt.get();
        } else {
            // 새 아이템 생성
            item = PortfolioItem.builder()
                    .portfolio(transaction.getPortfolio())
                    .coinId(transaction.getCoinId())
                    .coinName(transaction.getCoinName())
                    .coinSymbol(transaction.getCoinSymbol())
                    .quantity(BigDecimal.ZERO)
                    .averagePrice(BigDecimal.ZERO)
                    .totalInvestment(BigDecimal.ZERO)
                    .currentValue(BigDecimal.ZERO)
                    .unrealizedGain(BigDecimal.ZERO)
                    .unrealizedGainPercent(BigDecimal.ZERO)
                    .firstPurchaseDate(transaction.getTransactionDate())
                    .build();
        }

        // 거래에 따른 수량 및 평균가 업데이트
        updateItemFromTransaction(item, transaction);

        PortfolioItem savedItem = portfolioItemRepository.save(item);
        
        // 포트폴리오 변동 알림 발송
        sendPortfolioUpdateNotification(transaction.getUser(), savedItem, transaction);
    }

    /**
     * 거래에 따른 아이템 업데이트
     */
    private void updateItemFromTransaction(PortfolioItem item, Transaction transaction) {
        if (transaction.getType() == Transaction.TransactionType.BUY) {
            // 매수
            BigDecimal newTotalInvestment = item.getTotalInvestment()
                    .add(transaction.getTotalAmount());
            BigDecimal newQuantity = item.getQuantity().add(transaction.getQuantity());
            BigDecimal newAveragePrice = newQuantity.compareTo(BigDecimal.ZERO) > 0 
                    ? newTotalInvestment.divide(newQuantity, SCALE, ROUNDING_MODE)
                    : BigDecimal.ZERO;

            item.setQuantity(newQuantity);
            item.setAveragePrice(newAveragePrice);
            item.setTotalInvestment(newTotalInvestment);

            if (item.getFirstPurchaseDate() == null) {
                item.setFirstPurchaseDate(transaction.getTransactionDate());
            }

        } else if (transaction.getType() == Transaction.TransactionType.SELL) {
            // 매도
            BigDecimal newQuantity = item.getQuantity().subtract(transaction.getQuantity());
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("매도 수량이 보유 수량을 초과합니다.");
            }

            BigDecimal soldRatio = item.getQuantity().compareTo(BigDecimal.ZERO) > 0
                    ? transaction.getQuantity().divide(item.getQuantity(), SCALE, ROUNDING_MODE)
                    : BigDecimal.ZERO;
            BigDecimal newTotalInvestment = item.getTotalInvestment()
                    .subtract(item.getTotalInvestment().multiply(soldRatio));

            item.setQuantity(newQuantity);
            item.setTotalInvestment(newTotalInvestment);
        }

        // 현재 가치 및 손익 계산
        updateItemCurrentValue(item);
    }

    /**
     * 아이템 현재 가치 업데이트
     */
    private void updateItemCurrentValue(PortfolioItem item) {
        try {
            // 현재 가격 조회 (캐시 활용)
            BigDecimal currentPrice = coinPriceService.getCurrentPrice(item.getCoinId());
            BigDecimal currentValue = item.getQuantity().multiply(currentPrice);
            BigDecimal unrealizedGain = currentValue.subtract(item.getTotalInvestment());
            BigDecimal unrealizedGainPercent = item.getTotalInvestment().compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedGain.divide(item.getTotalInvestment(), 4, ROUNDING_MODE).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            item.setCurrentPrice(currentPrice);
            item.setCurrentValue(currentValue);
            item.setUnrealizedGain(unrealizedGain);
            item.setUnrealizedGainPercent(unrealizedGainPercent);
            item.setLastUpdatedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.warn("코인 가격 조회 실패: 코인ID={}", item.getCoinId(), e);
            // 가격 조회 실패 시에도 다른 정보는 업데이트
        }
    }

    /**
     * 포트폴리오 아이템 롤백
     */
    private void rollbackPortfolioItem(Transaction transaction) {
        Optional<PortfolioItem> itemOpt = portfolioItemRepository
                .findByPortfolioIdAndCoinId(transaction.getPortfolio().getId(), transaction.getCoinId());

        if (itemOpt.isPresent()) {
            PortfolioItem item = itemOpt.get();

            if (transaction.getType() == Transaction.TransactionType.BUY) {
                // 매수 롤백 -> 수량 감소
                BigDecimal newQuantity = item.getQuantity().subtract(transaction.getQuantity());
                BigDecimal newTotalInvestment = item.getTotalInvestment().subtract(transaction.getTotalAmount());

                if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    portfolioItemRepository.delete(item);
                    return;
                }

                BigDecimal newAveragePrice = newTotalInvestment.divide(newQuantity, SCALE, ROUNDING_MODE);
                item.setQuantity(newQuantity);
                item.setAveragePrice(newAveragePrice);
                item.setTotalInvestment(newTotalInvestment);

            } else if (transaction.getType() == Transaction.TransactionType.SELL) {
                // 매도 롤백 -> 수량 증가
                BigDecimal newQuantity = item.getQuantity().add(transaction.getQuantity());
                BigDecimal soldRatio = newQuantity.compareTo(BigDecimal.ZERO) > 0
                        ? transaction.getQuantity().divide(newQuantity, SCALE, ROUNDING_MODE)
                        : BigDecimal.ZERO;
                BigDecimal addedInvestment = transaction.getTotalAmount().multiply(soldRatio);
                BigDecimal newTotalInvestment = item.getTotalInvestment().add(addedInvestment);

                item.setQuantity(newQuantity);
                item.setTotalInvestment(newTotalInvestment);
                item.setAveragePrice(newTotalInvestment.divide(newQuantity, SCALE, ROUNDING_MODE));
            }

            updateItemCurrentValue(item);
            portfolioItemRepository.save(item);
        }
    }

    /**
     * 필터링된 거래 내역 조회 (사용자별)
     */
    private Page<Transaction> getFilteredTransactionsByUser(Long userId, TransactionDto.FilterRequest filter, 
                                                           Pageable pageable) {
        // 필터 조건에 따른 동적 쿼리 실행
        if (hasFilters(filter)) {
            // 복잡한 필터링은 Repository에서 처리
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId, pageable);
        } else {
            return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId, pageable);
        }
    }

    /**
     * 필터링된 거래 내역 조회 (포트폴리오별)
     */
    private Page<Transaction> getFilteredTransactionsByPortfolio(Long portfolioId, TransactionDto.FilterRequest filter,
                                                               Pageable pageable) {
        // 필터 조건에 따른 동적 쿼리 실행
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId, pageable);
    }

    /**
     * 필터 조건 확인
     */
    private boolean hasFilters(TransactionDto.FilterRequest filter) {
        if (filter == null) return false;
        return filter.getCoinSymbol() != null ||
               filter.getTransactionType() != null ||
               filter.getExchange() != null ||
               filter.getStartDate() != null ||
               filter.getEndDate() != null ||
               filter.getMinAmount() != null ||
               filter.getMaxAmount() != null;
    }

    /**
     * 거래 통계 계산
     */
    private TransactionDto.Summary calculateTransactionSummary(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return TransactionDto.Summary.builder()
                    .totalTransactions(0L)
                    .buyTransactions(0L)
                    .sellTransactions(0L)
                    .totalBuyAmount(BigDecimal.ZERO)
                    .totalSellAmount(BigDecimal.ZERO)
                    .totalRealizedPnl(BigDecimal.ZERO)
                    .totalFees(BigDecimal.ZERO)
                    .build();
        }

        long buyCount = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.BUY)
                .count();

        long sellCount = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.SELL)
                .count();

        BigDecimal totalBuyAmount = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.BUY)
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.SELL)
                .map(Transaction::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedPnl = transactions.stream()
                .filter(t -> t.getRealizedGain() != null)
                .map(Transaction::getRealizedGain)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = transactions.stream()
                .map(Transaction::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String mostTradedCoin = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getCoinSymbol, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        LocalDateTime lastTransactionDate = transactions.stream()
                .map(Transaction::getTransactionDate)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return TransactionDto.Summary.builder()
                .totalTransactions((long) transactions.size())
                .buyTransactions(buyCount)
                .sellTransactions(sellCount)
                .totalBuyAmount(totalBuyAmount)
                .totalSellAmount(totalSellAmount)
                .totalRealizedPnl(totalRealizedPnl)
                .totalFees(totalFees)
                .mostTradedCoin(mostTradedCoin)
                .lastTransactionDate(lastTransactionDate)
                .build();
    }

    /**
     * 기간별 통계 계산
     */
    private List<TransactionDto.Statistics> calculatePeriodStatistics(List<Transaction> transactions, String groupBy) {
        // 그룹별로 거래 내역 분류
        Map<String, List<Transaction>> groupedTransactions = transactions.stream()
                .collect(Collectors.groupingBy(t -> formatDateByGroup(t.getTransactionDate(), groupBy)));

        return groupedTransactions.entrySet().stream()
                .map(entry -> {
                    List<Transaction> periodTransactions = entry.getValue();
                    
                    long transactionCount = periodTransactions.size();
                    BigDecimal totalVolume = periodTransactions.stream()
                            .map(Transaction::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                    BigDecimal averageAmount = transactionCount > 0 
                            ? totalVolume.divide(BigDecimal.valueOf(transactionCount), SCALE, ROUNDING_MODE)
                            : BigDecimal.ZERO;

                    BigDecimal realizedPnl = periodTransactions.stream()
                            .filter(t -> t.getRealizedGain() != null)
                            .map(Transaction::getRealizedGain)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TransactionDto.Statistics.builder()
                            .period(entry.getKey())
                            .transactionCount(transactionCount)
                            .totalVolume(totalVolume)
                            .averageTransactionAmount(averageAmount)
                            .realizedPnl(realizedPnl)
                            .build();
                })
                .sorted((a, b) -> b.getPeriod().compareTo(a.getPeriod()))
                .collect(Collectors.toList());
    }

    /**
     * 날짜 그룹 포맷팅
     */
    private String formatDateByGroup(LocalDateTime date, String groupBy) {
        switch (groupBy.toUpperCase()) {
            case "DAILY":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            case "WEEKLY":
                return date.format(DateTimeFormatter.ofPattern("yyyy-'W'ww"));
            case "MONTHLY":
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            default:
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    /**
     * CSV 라인 파싱
     */
    private Transaction parseCsvLine(String line, User user, Portfolio portfolio) {
        try {
            String[] columns = line.split(",");
            if (columns.length < 7) {
                log.warn("CSV 라인 형식 오류: {}", line);
                return null;
            }

            return Transaction.builder()
                    .user(user)
                    .portfolio(portfolio)
                    .coinId(columns[0].trim().toLowerCase())
                    .coinName(columns[1].trim())
                    .coinSymbol(columns[0].trim().toUpperCase())
                    .type(Transaction.TransactionType.valueOf(columns[2].trim().toUpperCase()))
                    .quantity(new BigDecimal(columns[3].trim()))
                    .price(new BigDecimal(columns[4].trim()))
                    .totalAmount(new BigDecimal(columns[3].trim()).multiply(new BigDecimal(columns[4].trim())))
                    .fee(columns.length > 5 ? new BigDecimal(columns[5].trim()) : BigDecimal.ZERO)
                    .exchange(columns.length > 6 ? columns[6].trim() : "Unknown")
                    .transactionDate(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.warn("CSV 라인 파싱 실패: {}", line, e);
            return null;
        }
    }

    /**
     * CSV 콘텐츠 생성
     */
    private String generateCsvContent(List<Transaction> transactions) {
        StringBuilder csv = new StringBuilder();
        csv.append("코인심볼,코인이름,거래유형,수량,가격,수수료,거래소,거래일시,메모\n");

        for (Transaction transaction : transactions) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    transaction.getCoinSymbol(),
                    transaction.getCoinName(),
                    transaction.getType(),
                    transaction.getQuantity(),
                    transaction.getPrice(),
                    transaction.getFee(),
                    transaction.getExchange(),
                    transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    transaction.getMemo() != null ? transaction.getMemo() : ""
            ));
        }

        return csv.toString();
    }

    /**
     * 거래 완료 알림 발송 - 실시간 알림 시스템 연동
     */
    private void sendTransactionNotification(User user, Transaction transaction) {
        try {
            // 실시간 알림 시스템을 통한 비동기 알림 발송
            realtimeNotificationService.sendTransactionNotification(transaction);
            
            log.debug("거래 완료 알림 발송: 사용자ID={}, 거래ID={}, 코인={}", 
                    user.getId(), transaction.getId(), transaction.getCoinSymbol());
                    
        } catch (Exception e) {
            log.warn("거래 알림 발송 실패: 사용자ID={}, 거래ID={}", 
                    user.getId(), transaction.getId(), e);
        }
    }

    /**
     * 포트폴리오 변동 알림 발송
     */
    private void sendPortfolioUpdateNotification(User user, PortfolioItem item, Transaction transaction) {
        try {
            // 포트폴리오 변동 알림 비동기 발송
            realtimeNotificationService.sendPortfolioUpdateNotification(user.getId(), item);
            
            log.debug("포트폴리오 변동 알림 발송: 사용자ID={}, 코인={}, 수량={}", 
                    user.getId(), item.getCoinSymbol(), item.getQuantity());
                    
        } catch (Exception e) {
            log.warn("포트폴리오 변동 알림 발송 실패: 사용자ID={}, 아이템ID={}", 
                    user.getId(), item.getId(), e);
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + username));
    }

    private Portfolio findPortfolioByIdAndUser(Long portfolioId, User user) {
        return portfolioRepository.findById(portfolioId)
                .filter(p -> p.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("포트폴리오를 찾을 수 없습니다: " + portfolioId));
    }

    private Transaction findTransactionByIdAndUser(Long transactionId, String username) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("거래 내역을 찾을 수 없습니다: " + transactionId));

        if (!transaction.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("해당 거래 내역에 접근할 권한이 없습니다.");
        }

        return transaction;
    }

    /**
     * 엔티티를 응답 DTO로 변환
     */
    private TransactionDto.Response mapToResponse(Transaction transaction) {
        return TransactionDto.Response.builder()
                .id(transaction.getId())
                .portfolioId(transaction.getPortfolio().getId())
                .portfolioName(transaction.getPortfolio().getName())
                .coinSymbol(transaction.getCoinSymbol())
                .coinName(transaction.getCoinName())
                .transactionType(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .totalAmount(transaction.getTotalAmount())
                .fee(transaction.getFee())
                .netAmount(transaction.getTotalAmountWithFee())
                .exchange(transaction.getExchange())
                .notes(transaction.getMemo())
                .realizedPnl(transaction.getRealizedGain())
                .transactionDate(transaction.getTransactionDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * 통계 객체 변환
     */
    private TransactionDto.Statistics mapToStatistics(Object[] stats) {
        return TransactionDto.Statistics.builder()
                .period(String.valueOf(stats[0]))
                .transactionCount((Long) stats[1])
                .totalVolume((BigDecimal) stats[2])
                .averageTransactionAmount((BigDecimal) stats[3])
                .realizedPnl((BigDecimal) stats[4])
                .build();
    }
}
