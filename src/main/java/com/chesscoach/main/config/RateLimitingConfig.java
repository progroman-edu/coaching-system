package com.chesscoach.main.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor()).addPathPatterns("/api/**");
    }

    public static class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        private static final Bucket bucket = Bucket4j.builder()
            .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
            .build();

        @Override
        public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, 
                                jakarta.servlet.http.HttpServletResponse response, 
                                Object handler) throws Exception {
            if (bucket.tryConsume(1)) {
                return true;
            }
            response.setStatus(429);
            response.getWriter().write("Too many requests. Rate limit exceeded.");
            return false;
        }
    }
}
