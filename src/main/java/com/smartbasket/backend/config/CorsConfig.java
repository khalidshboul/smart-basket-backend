package com.smartbasket.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Parse allowed origins from environment variable
        // Format: "origin1,origin2,origin3" or "*" for all
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        
        if (origins.size() == 1 && "*".equals(origins.get(0).trim())) {
            // Allow all origins (development mode)
            config.setAllowedOriginPatterns(Arrays.asList("*"));
        } else {
            // Allow specific origins (production mode)
            origins.forEach(origin -> config.addAllowedOrigin(origin.trim()));
        }
        
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
