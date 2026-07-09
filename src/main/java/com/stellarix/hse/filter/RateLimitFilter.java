package com.stellarix.hse.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    // Both are brute-forceable secrets (password, TOTP code) — each gets its own
    // budget per IP so hammering one doesn't consume the other's headroom.
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/hse/signin",
            "/hse/users/totp/enable"
    );
    private static final int MAX_ATTEMPTS = 5;

    private final LoadingCache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> Bucket.builder()
                    .addLimit(Bandwidth.classic(MAX_ATTEMPTS, Refill.intervally(MAX_ATTEMPTS, Duration.ofMinutes(1))))
                    .build());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!RATE_LIMITED_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.get(resolveClientIp(request) + ":" + path);
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too many requests\",\"message\":\"Too many login attempts. Please wait 1 minute.\",\"status\":429}"
            );
        }
    }

    // There is no trusted reverse proxy in front of this backend today that can be
    // relied on to strip/overwrite a client-supplied X-Forwarded-For — trusting it
    // unconditionally would let any client mint a fresh rate-limit bucket per request
    // just by spoofing the header. Use the actual socket address instead.
    private String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
