package com.mybeaufortviewproject.mybeaufortview_backend.infrastructure.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybeaufortviewproject.mybeaufortview_backend.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@EnableMethodSecurity
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    // Dependencies
    private final JwtUtil jwtUtil;
    private final JwtAuthEntryPoint jwtAuthEntryPoint;
    private final UserRepository userRepository;

    @Autowired(required = false) // Optional injection for rate limiting
    private RateLimitingFilter rateLimitingFilter;

    // Constructor injection
    public SecurityConfig(JwtUtil jwtUtil, JwtAuthEntryPoint jwtAuthEntryPoint, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.jwtAuthEntryPoint = jwtAuthEntryPoint;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth

                        // --- ACTUATORS ---
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/info").permitAll()

                        .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                        .requestMatchers("/actuator/metrics/**").hasRole("ADMIN")

                        // All other actuator endpoints require ADMIN role for security hardening, even if not currently exposed
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Publicly accessible from registration and login endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // --- OPENAPI / SWAGGER ----
                        .requestMatchers(
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
                            "/v3/api-docs.yaml", "/favicon.ico", "/error").permitAll()

                        // --- STATIC RESOURCES ----
                        .requestMatchers("/static/**", "/webjars/**").permitAll()

                        // Allow preflight requests to all endpoints so that CORS works properly
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow public access to error path
                        .requestMatchers("/error").permitAll()

                        // --- USERS ----
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/*").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.GET, "/api/users/*/profile").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/posts").permitAll()

                        // --- POSTS ----
                        // Personalized: needs authentication (depending on current user)
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/liked").hasAnyRole("ADMIN", "PRIVILEGED_USER")

                        // Like/unlike handled in Likes section
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/like").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*/like").hasAnyRole("ADMIN", "PRIVILEGED_USER")

                        // Count is public
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/likes/count").permitAll()

                        // Post interactions
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()

                        // Other post operations remain protected
                        .requestMatchers(HttpMethod.POST, "/api/posts").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasAnyRole("ADMIN", "PRIVILEGED_USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasAnyRole("ADMIN", "PRIVILEGED_USER")

                        // --- COLLECTIONS ---
                        .requestMatchers(HttpMethod.GET, "/api/collections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/*/collections").permitAll()

                        // --- LOCATIONS ---
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()

                        // --- SEARCH ---
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()

                        // --- S3 UPLOADS ---
                        .requestMatchers(HttpMethod.POST, "/api/uploads/**").authenticated()

                        // --- MEDIA JOBS ---
                        .requestMatchers(HttpMethod.GET, "/api/media/**").permitAll()

                        // --- NOTIFICATIONS ---
                        .requestMatchers(HttpMethod.GET, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/notifications/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/notifications/**").authenticated()

                        // --- COMMISSION REQUESTS ---
                        .requestMatchers(HttpMethod.GET, "/api/commissions/**").authenticated()
                        .anyRequest().authenticated())

                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());

        // Add rate limiting BEFORE JWT
        if (rateLimitingFilter != null) {
            http.addFilterBefore(
                rateLimitingFilter,
                UsernamePasswordAuthenticationFilter.class
        );
        }

        // Existing JWT filter
        http.addFilterBefore(
                jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            HttpSecurity http,
            CustomUserDetailsService userDetailService,
            PasswordEncoder passwordEncoder)
                    throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailService)
        .passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:3000", "https://mybeaufortview.netlify.app", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("status", 403);
            responseBody.put("error", "Forbidden");
            responseBody.put("message", "Access denied: Insufficient permissions");
            responseBody.put("path", request.getRequestURI());

            new ObjectMapper().writeValue(response.getWriter(), responseBody);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil, userRepository);
    }
}
