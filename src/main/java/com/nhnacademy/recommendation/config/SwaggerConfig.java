package com.nhnacademy.recommendation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {

    @Value("${swagger.ui.allowed-origin:http://localhost:8089}")
    private String allowedOrigin;

    @Bean
    public OpenAPI recommendationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("4iren Recommendation API")
                        .description("4iren 추천, LLM 챗, 웰컴 브리핑 API 문서")
                        .version("v1"));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v3/api-docs/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "OPTIONS");
    }
}
