package com.smartbasket.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Autowired
    private Environment env;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Get allowed origins from environment variable (CORS_ALLOWED_ORIGINS)
        // Try environment variable first, then system property, then default
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = System.getProperty("CORS_ALLOWED_ORIGINS");
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = "*";
        }
        
        // Log the value for debugging (remove in production)
        System.out.println("CORS_ALLOWED_ORIGINS value: " + allowedOrigins);
        
        // Format: "origin1,origin2,origin3" or "*" for all
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        String trimmedFirst = origins.get(0).trim();
        
        System.out.println("Processing CORS - first origin: '" + trimmedFirst + "', is star: " + "*".equals(trimmedFirst));
        
        if (origins.size() == 1 && "*".equals(trimmedFirst)) {
            // Allow all origins (development mode)
            // Note: Cannot use allowCredentials(true) with "*" - Spring Boot throws error
            System.out.println("Using wildcard CORS (dev mode)");
            registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .exposedHeaders("Content-Type", "Authorization");
        } else {
            // Allow specific origins (production mode)
            // Use allowedOriginPatterns instead of allowedOrigins to avoid the credentials issue
            String[] originArray = origins.stream()
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
            
            System.out.println("Using specific origins CORS: " + Arrays.toString(originArray));
            
            // Use allowedOriginPatterns which works with credentials
            registry.addMapping("/**")
                .allowedOriginPatterns(originArray)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers")
                .allowCredentials(true)
                .maxAge(3600) // Cache preflight requests for 1 hour
                .exposedHeaders("Content-Type", "Authorization");
        }
    }
}
