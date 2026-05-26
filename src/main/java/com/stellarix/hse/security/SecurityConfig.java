package com.stellarix.hse.security;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.stellarix.hse.filter.JwtAuthFilter;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
	
	@Lazy
	private final JwtAuthFilter jwtAuthFilter;
	
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${frontend.url}")
    private String frontendUrl; 
    
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
        	.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
        	.csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                // Auth
                .requestMatchers("/hse/test", "/hse/signin", "/hse/refresh_token", "/hse/logout", "/hse/verify_token").permitAll()
                // WebSocket
                .requestMatchers("/hse/ws/**").permitAll()
                // Mobile and backoffice — read-only lookups (no auth required)
                .requestMatchers(HttpMethod.GET, "/hse/sites", "/hse/ppe-items", "/hse/ppe-matrix", "/hse/ppe-matrix/mobile-cache", "/hse/inductions/verify", "/hse/inductions/names", "/hse/zone-types", "/hse/habilitations", "/hse/permits/verify").permitAll()
                // Mobile — cloud inference (no auth required, image never stored here)
                .requestMatchers(HttpMethod.POST, "/hse/verifications/analyze").permitAll()
                // Mobile — submit verification + offline sync + rejected image upload + certify
                .requestMatchers(HttpMethod.POST, "/hse/verifications", "/hse/verifications/sync").permitAll()
                .requestMatchers(HttpMethod.POST, "/hse/verifications/*/image").permitAll()
                .requestMatchers(HttpMethod.POST, "/hse/verifications/*/images").permitAll()
                .requestMatchers(HttpMethod.GET, "/hse/verifications/*/composite-image").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/hse/verifications/*/certify").permitAll()
                // Everything else requires HSE authority
                .requestMatchers("/hse/**").hasAuthority("HSE")
                .anyRequest().authenticated()
            )

            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authenticationProvider(authenticationProvider())
            
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    
    @Bean
    protected CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration().applyPermitDefaultValues();

        String origin = frontendUrl.trim();
        if (origin.endsWith("/")) origin = origin.substring(0, origin.length() - 1);
        
        configuration.setAllowedOrigins(List.of(origin));
        configuration.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true); // Allow credentials (cookies, authorization headers)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
        
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
    
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
}
