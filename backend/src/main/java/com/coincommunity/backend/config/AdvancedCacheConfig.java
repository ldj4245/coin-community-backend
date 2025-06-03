package com.coincommunity.backend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 고급 캐시 설정
 * 
 * 30년차 베테랑급 캐시 전략:
 * - 다층 캐시 아키텍처
 * - 캐시별 TTL 최적화
 * - 직렬화 성능 최적화
 * - 캐시 히트율 모니터링
 * - 메모리 효율성 관리
 * 
 * @author CoinCommunity Backend Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class AdvancedCacheConfig {

    @Value("${app.cache.default-ttl:3600}")
    private long defaultTtlSeconds;

    @Value("${app.cache.coin-price-ttl:60}")
    private long coinPriceTtlSeconds;

    @Value("${app.cache.user-data-ttl:1800}")
    private long userDataTtlSeconds;

    @Value("${app.cache.transaction-ttl:7200}")
    private long transactionTtlSeconds;

    @Value("${app.cache.portfolio-ttl:1800}")
    private long portfolioTtlSeconds;

    @Value("${app.cache.statistics-ttl:900}")
    private long statisticsTtlSeconds;

    /**
     * Redis 기반 캐시 매니저 설정
     * 캐시별 TTL과 직렬화 전략 최적화
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // JSON 직렬화 설정
        ObjectMapper objectMapper = createOptimizedObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(defaultTtlSeconds))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "coin-community:" + cacheName + ":");

        // 캐시별 개별 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = createCacheConfigurations(jsonSerializer);

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        log.info("고급 Redis 캐시 매니저 초기화 완료 - 캐시 수: {}", cacheConfigurations.size());
        
        return cacheManager;
    }

    /**
     * 캐시별 개별 설정 생성
     */
    private Map<String, RedisCacheConfiguration> createCacheConfigurations(GenericJackson2JsonRedisSerializer jsonSerializer) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // 기본 설정 템플릿
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .computePrefixWith(cacheName -> "coin-community:" + cacheName + ":");

        // 코인 가격 캐시 (짧은 TTL, 높은 갱신율)
        configs.put("coinPrices", baseConfig
                .entryTtl(Duration.ofSeconds(coinPriceTtlSeconds))
                .prefixCacheNameWith("price:"));

        configs.put("coinPriceHistory", baseConfig
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("price-history:"));

        // 사용자 데이터 캐시
        configs.put("userProfiles", baseConfig
                .entryTtl(Duration.ofSeconds(userDataTtlSeconds))
                .prefixCacheNameWith("user:"));

        configs.put("userTransactions", baseConfig
                .entryTtl(Duration.ofSeconds(transactionTtlSeconds))
                .prefixCacheNameWith("user-tx:"));

        configs.put("userNotificationEligible", baseConfig
                .entryTtl(Duration.ofHours(24))
                .prefixCacheNameWith("user-notif:"));

        // 포트폴리오 캐시
        configs.put("portfolios", baseConfig
                .entryTtl(Duration.ofSeconds(portfolioTtlSeconds))
                .prefixCacheNameWith("portfolio:"));

        configs.put("portfolioTransactions", baseConfig
                .entryTtl(Duration.ofSeconds(transactionTtlSeconds))
                .prefixCacheNameWith("portfolio-tx:"));

        configs.put("portfolioItems", baseConfig
                .entryTtl(Duration.ofSeconds(portfolioTtlSeconds))
                .prefixCacheNameWith("portfolio-items:"));

        // 거래 내역 캐시
        configs.put("transactionDetails", baseConfig
                .entryTtl(Duration.ofSeconds(transactionTtlSeconds))
                .prefixCacheNameWith("tx-detail:"));

        configs.put("transactionStats", baseConfig
                .entryTtl(Duration.ofSeconds(statisticsTtlSeconds))
                .prefixCacheNameWith("tx-stats:"));

        // 관심종목 캐시
        configs.put("coinWatchlists", baseConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("watchlist:"));

        // 코인 분석 캐시
        configs.put("coinAnalysis", baseConfig
                .entryTtl(Duration.ofMinutes(15))
                .prefixCacheNameWith("analysis:"));

        // 사용자 점수 캐시
        configs.put("userScores", baseConfig
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("score:"));

        // 통계 캐시 (긴 TTL)
        configs.put("marketStatistics", baseConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("market-stats:"));

        configs.put("topCoins", baseConfig
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("top-coins:"));

        log.info("캐시 설정 완료 - 총 {}개 캐시 타입", configs.size());

        return configs;
    }

    /**
     * 성능 최적화된 ObjectMapper 생성
     */
    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Java 8 시간 타입 지원
        mapper.registerModule(new JavaTimeModule());
        
        // 타입 정보 포함하여 역직렬화 안정성 확보
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );
        
        return mapper;
    }

    /**
     * Redis Template 설정 (일반 용도)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        ObjectMapper objectMapper = createOptimizedObjectMapper();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        log.info("Redis Template 초기화 완료");
        
        return template;
    }

    /**
     * WebSocket 세션 관리용 Redis Template
     */
    @Bean(name = "webSocketRedisTemplate")
    public RedisTemplate<String, String> webSocketRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 문자열 직렬화 (WebSocket 세션 ID는 문자열)
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        
        log.info("WebSocket 전용 Redis Template 초기화 완료");
        
        return template;
    }
}
