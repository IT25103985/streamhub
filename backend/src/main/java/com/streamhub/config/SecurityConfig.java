package com.streamhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
//spring security configuration defining route access rules and stateless session
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout", "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/videos", "/api/v1/videos/**", "/api/videos", "/api/videos/**", "/api/v1/video").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/videos/*/view").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**", "/api/categories", "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/playlists", "/api/v1/playlists/**", "/api/playlists", "/api/playlists/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/comments", "/api/v1/likes/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/help/**", "/api/help/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/avatar/**", "/api/v1/users/cover/**", "/api/v1/users/*/profile").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/watch-history/trending", "/api/watch-history/trending").permitAll()
                .requestMatchers("/api/v1/admin/users/**", "/api/admin/users/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/api/v1/admin/analytics/**", "/api/admin/analytics/**").hasRole("ADMINISTRATOR")
                .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasAnyRole("ADMINISTRATOR", "CONTENT_MODERATOR")
                .requestMatchers("/api/v1/moderate/**", "/api/moderate/**").hasAnyRole("ADMINISTRATOR", "CONTENT_MODERATOR")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:3000", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "x-auth-token", "Range"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token", "Accept-Ranges", "Content-Range", "Content-Length", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}