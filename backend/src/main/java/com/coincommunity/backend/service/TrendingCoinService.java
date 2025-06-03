package com.coincommunity.backend.service;

import com.coincommunity.backend.dto.ExchangePriceDto;
import com.coincommunity.backend.dto.TrendingCoinDto;
import com.coincommunity.backend.entity.Post;
import com.coincommunity.backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 트렌딩 코인 서비스
 * 가격 변동과 커뮤니티 언급을 기반으로 트렌딩 코인을 계산
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingCoinService {

    private final ExchangePriceService exchangePriceService;
    private final PostRepository postRepository;

    // 코인 심볼과 이름 매핑 (대소문자 구분 없이)
    private final Map<String, String> coinNameMap = new ConcurrentHashMap<>();

    // 코인 언급 횟수 캐시
    private final Map<String, Integer> coinMentionCache = new ConcurrentHashMap<>();

    // 트렌딩 코인 캐시
    private List<TrendingCoinDto> trendingCoinsCache = new ArrayList<>();

    /**
     * 초기화 - 지원하는 코인 목록 로드
     */
    @Scheduled(fixedRate = 86400000) // 24시간마다 실행
    public void initCoinMap() {
        log.info("코인 맵 초기화 시작");

        // 모든 거래소에서 지원하는 코인 목록 가져오기
        List<String> allCoins = exchangePriceService.getAllSupportedCoins();

        // 코인 이름 매핑 업데이트
        for (String coin : allCoins) {
            coinNameMap.put(coin.toLowerCase(), coin);
        }

        // 주요 코인 수동 추가 (API에서 누락된 경우를 대비)
        coinNameMap.putIfAbsent("btc", "BTC");
        coinNameMap.putIfAbsent("eth", "ETH");
        coinNameMap.putIfAbsent("xrp", "XRP");
        coinNameMap.putIfAbsent("sol", "SOL");
        coinNameMap.putIfAbsent("ada", "ADA");
        coinNameMap.putIfAbsent("doge", "DOGE");

        log.info("코인 맵 초기화 완료 - 총 코인 수: {}", coinNameMap.size());
    }

    /**
     * 트렌딩 코인 목록 조회
     */
    @Cacheable(value = "trendingCoins", key = "#limit + '-' + #sortBy")
    public List<TrendingCoinDto> getTrendingCoins(int limit, TrendingCoinDto.SortBy sortBy) {
        log.info("트렌딩 코인 조회 시작 - 개수: {}, 정렬: {}", limit, sortBy);

        // 캐시가 비어있으면 업데이트
        if (trendingCoinsCache.isEmpty()) {
            updateTrendingCoins();
        }

        // 정렬 기준에 따라 정렬
        List<TrendingCoinDto> sortedCoins = sortTrendingCoins(trendingCoinsCache, sortBy);

        // 요청된 개수만큼 반환
        List<TrendingCoinDto> result = sortedCoins.stream()
                .limit(limit)
                .collect(Collectors.toList());

        log.info("트렌딩 코인 조회 완료 - 반환된 코인 수: {}", result.size());
        return result;
    }

    /**
     * 트렌딩 코인 업데이트 (스케줄러에서 호출)
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void updateTrendingCoins() {
        log.info("트렌딩 코인 업데이트 시작");

        try {
            // 시가총액 상위 코인 가져오기
            List<ExchangePriceDto> topCoins = exchangePriceService.getTopCoinsByMarketCap(100);

            // 커뮤니티 언급 횟수 업데이트
            updateCommunityMentions();

            // 트렌딩 코인 계산
            List<TrendingCoinDto> trendingCoins = calculateTrendingCoins(topCoins);

            // 캐시 업데이트
            this.trendingCoinsCache = trendingCoins;

            log.info("트렌딩 코인 업데이트 완료 - 총 코인 수: {}", trendingCoins.size());
        } catch (Exception e) {
            log.error("트렌딩 코인 업데이트 실패", e);
        }
    }

    /**
     * 커뮤니티 언급 횟수 업데이트
     */
    private void updateCommunityMentions() {
        log.info("커뮤니티 언급 횟수 업데이트 시작");

        // 최근 게시글 조회 (최근 10개)
        List<Post> recentPosts = postRepository.findTop10ByOrderByCreatedAtDesc();

        // 사용자별 게시글도 추가로 조회
        List<Post> userPosts = new ArrayList<>();
        for (Long userId = 1L; userId <= 10L; userId++) {  // 임의로 10명의 사용자 게시글 조회
            try {
                userPosts.addAll(postRepository.findByUserId(userId));
            } catch (Exception e) {
                // 사용자가 없거나 게시글이 없는 경우 무시
            }
        }

        // 중복 제거 및 합치기
        Set<Post> allPosts = new HashSet<>(recentPosts);
        allPosts.addAll(userPosts);

        // 언급 횟수 초기화
        coinMentionCache.clear();

        // 각 게시글에서 코인 언급 횟수 계산
        for (Post post : allPosts) {
            countCoinMentions(post.getTitle() + " " + post.getContent());
        }

        log.info("커뮤니티 언급 횟수 업데이트 완료 - 언급된 코인 수: {}", coinMentionCache.size());
    }

    /**
     * 텍스트에서 코인 언급 횟수 계산
     */
    private void countCoinMentions(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // 소문자로 변환
        String lowerText = text.toLowerCase();

        // 각 코인 심볼에 대해 언급 횟수 계산
        for (String coinSymbol : coinNameMap.keySet()) {
            // 단어 경계를 가진 패턴 (예: "btc"는 "btc", "btc.", "btc," 등과 매치, "xbtc"와는 매치하지 않음)
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(coinSymbol) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(lowerText);

            int count = 0;
            while (matcher.find()) {
                count++;
            }

            if (count > 0) {
                // 기존 언급 횟수에 추가
                coinMentionCache.merge(coinSymbol, count, Integer::sum);
            }
        }
    }

    /**
     * 트렌딩 코인 계산
     */
    private List<TrendingCoinDto> calculateTrendingCoins(List<ExchangePriceDto> topCoins) {
        List<TrendingCoinDto> result = new ArrayList<>();

        for (ExchangePriceDto coin : topCoins) {
            String symbol = coin.getSymbol();
            String lowerSymbol = symbol.toLowerCase();

            // 커뮤니티 언급 횟수 (없으면 0)
            int mentionCount = coinMentionCache.getOrDefault(lowerSymbol, 0);

            // 트렌딩 점수 계산 (가격 변동 + 거래량 + 커뮤니티 언급)
            int trendingScore = calculateTrendingScore(coin, mentionCount);

            // 시가총액 계산 (현재가 * 발행량으로 추정, 실제로는 API에서 제공해야 함)
            BigDecimal estimatedMarketCap = null;
            if (coin.getCurrentPrice() != null) {
                // 임의의 발행량 값 (실제로는 API에서 제공해야 함)
                BigDecimal estimatedSupply = new BigDecimal("1000000"); 
                estimatedMarketCap = coin.getCurrentPrice().multiply(estimatedSupply);
            }

            TrendingCoinDto trendingCoin = TrendingCoinDto.builder()
                    .symbol(symbol)
                    .name(symbol) // 심볼을 이름으로 사용
                    .koreanName(coin.getKoreanName())
                    .currentPrice(coin.getCurrentPrice())
                    .priceChangePercent24h(coin.getChangeRate())
                    .volume24h(coin.getVolume24h())
                    .marketCap(estimatedMarketCap)
                    .communityMentionCount(mentionCount)
                    .trendingScore(trendingScore)
                    .imageUrl("/images/coins/" + symbol.toLowerCase() + ".png") // 기본 이미지 경로
                    .lastUpdated(LocalDateTime.now())
                    .build();

            result.add(trendingCoin);
        }

        // 트렌딩 점수로 정렬
        return sortTrendingCoins(result, TrendingCoinDto.SortBy.TRENDING_SCORE);
    }

    /**
     * 트렌딩 점수 계산
     */
    private int calculateTrendingScore(ExchangePriceDto coin, int mentionCount) {
        // 가격 변동 점수 (절대값 * 10)
        int priceChangeScore = 0;
        if (coin.getChangeRate() != null) {
            priceChangeScore = (int) Math.abs(coin.getChangeRate().doubleValue() * 10);
        }

        // 거래량 점수 (로그 스케일)
        int volumeScore = 0;
        if (coin.getVolume24h() != null && coin.getVolume24h().doubleValue() > 0) {
            volumeScore = (int) Math.log10(coin.getVolume24h().doubleValue()) * 10;
        }

        // 커뮤니티 언급 점수 (언급 횟수 * 20)
        int mentionScore = mentionCount * 20;

        // 총점
        return priceChangeScore + volumeScore + mentionScore;
    }

    /**
     * 트렌딩 코인 정렬
     */
    private List<TrendingCoinDto> sortTrendingCoins(List<TrendingCoinDto> coins, TrendingCoinDto.SortBy sortBy) {
        Comparator<TrendingCoinDto> comparator;

        switch (sortBy) {
            case PRICE_CHANGE:
                // 가격 변동률 (절대값) 기준 내림차순
                comparator = Comparator.comparing(
                        coin -> coin.getPriceChangePercent24h() != null ? 
                                Math.abs(coin.getPriceChangePercent24h().doubleValue()) : 0.0,
                        Comparator.reverseOrder());
                break;
            case VOLUME:
                // 거래량 기준 내림차순
                comparator = Comparator.comparing(
                        coin -> coin.getVolume24h() != null ? coin.getVolume24h().doubleValue() : 0.0,
                        Comparator.reverseOrder());
                break;
            case MARKET_CAP:
                // 시가총액 기준 내림차순
                comparator = Comparator.comparing(
                        coin -> coin.getMarketCap() != null ? coin.getMarketCap().doubleValue() : 0.0,
                        Comparator.reverseOrder());
                break;
            case COMMUNITY_MENTIONS:
                // 커뮤니티 언급 횟수 기준 내림차순
                comparator = Comparator.comparing(
                        coin -> coin.getCommunityMentionCount() != null ? coin.getCommunityMentionCount() : 0,
                        Comparator.reverseOrder());
                break;
            case TRENDING_SCORE:
            default:
                // 트렌딩 점수 기준 내림차순
                comparator = Comparator.comparing(
                        coin -> coin.getTrendingScore() != null ? coin.getTrendingScore() : 0,
                        Comparator.reverseOrder());
                break;
        }

        return coins.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
}
