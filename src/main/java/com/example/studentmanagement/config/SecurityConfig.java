package com.example.studentmanagement.config;

import com.example.studentmanagement.exception.ApiError;
import com.example.studentmanagement.security.JwtAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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

import java.time.Instant;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigin;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          ObjectMapper objectMapper,
                          @Value("${app.cors.allowed-origin}") String allowedOrigin) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigin = allowedOrigin;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())

            // CSRF is disabled ON PURPOSE: this API is stateless and authenticates with a Bearer token
            // sent in the Authorization header. Browsers do not attach that header automatically
            // (unlike cookies), so a malicious site cannot forge an authenticated request.
            // If tokens were ever stored in cookies, CSRF protection would have to be re-enabled.
            .csrf(csrf -> csrf.disable())

            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // CORS preflight
                .requestMatchers("/api/auth/**").permitAll()              // register, login, refresh, logout
                .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Student authorization rules, to enable when student endpoints are implemented:
                // GET /api/students/** -> ADMIN, TEACHER, STUDENT
                // POST and PUT /api/students/** -> ADMIN, TEACHER
                // DELETE /api/students/** -> ADMIN
                .anyRequest().authenticated())

            .exceptionHandling(ex -> ex
                // No/invalid token -> 401 (we don't know who you are)
                .authenticationEntryPoint((req, res, e) ->
                        writeError(req, res, HttpStatus.UNAUTHORIZED, "Authentication required: missing or invalid access token"))
                // Valid token but wrong role -> 403 (we know who you are, you're not allowed)
                .accessDeniedHandler((req, res, e) ->
                        writeError(req, res, HttpStatus.FORBIDDEN, "You do not have permission to perform this action")))

            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false); // no cookies involved
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(HttpServletRequest req, HttpServletResponse res,
                            HttpStatus status, String message) throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(),
                new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                        message, req.getRequestURI(), null));
    }
}
