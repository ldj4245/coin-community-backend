package com.coincommunity.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaAuditing
@EntityScan(basePackages = "com.coincommunity.backend.entity")
@EnableJpaRepositories(basePackages = "com.coincommunity.backend.repository")
public class CoinCommunityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoinCommunityBackendApplication.class, args);
    }
}
