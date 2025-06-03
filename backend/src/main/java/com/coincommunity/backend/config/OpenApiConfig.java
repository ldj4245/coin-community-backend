package com.coincommunity.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI (Swagger) 문서 설정
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(Arrays.asList(
                        new Server().url("/").description("현재 서버"),
                        new Server().url("https://api.coincommunity.com").description("운영 서버")
                ))
                // Bearer 토큰 인증 방식 설정
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력해주세요. 형식: Bearer {token}")));
    }

    private Info apiInfo() {
        return new Info()
                .title("코인 커뮤니티 API 문서")
                .description("코인 정보 조회, 게시글, 댓글, 좋아요, 뉴스 기능을 제공하는 코인 커뮤니티 백엔드 API")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("코인 커뮤니티 개발팀")
                        .email("support@coincommunity.com")
                        .url("https://coincommunity.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0"));
    }
}
