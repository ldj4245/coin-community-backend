package com.coincommunity.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableCaching
@EntityScan(basePackages = "com.coincommunity.backend.entity")
@EnableJpaRepositories(basePackages = "com.coincommunity.backend.repository")
public class CoinCommunityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinCommunityBackendApplication.class, args);
    }
}
