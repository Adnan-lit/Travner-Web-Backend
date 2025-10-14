package org.adnan.travner.config;

import org.adnan.travner.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SpringSecurity {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request -> request
                        // Allow CORS preflight requests globally
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints - no authentication required
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // WebSocket endpoints
                        .requestMatchers("/ws/**", "/chat/**").permitAll()

                        // Public content access (read-only) - GET requests only
                        .requestMatchers(HttpMethod.GET, "/api/posts", "/api/posts/", "/api/posts/**").permitAll()
                        // Align with implemented CommentController path
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/comments/**").permitAll()
                        // Backward-compat comment path (if present in docs/Postman)
                        .requestMatchers(HttpMethod.GET, "/api/comments/posts/**").permitAll()

                        // Public marketplace access (read-only) - GET requests only
                        .requestMatchers(HttpMethod.GET, "/api/market/products").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/category/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/location/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/tags").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/seller/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/market/products/{id}").permitAll()

                        // Public media access - GET requests only
                        .requestMatchers(HttpMethod.GET, "/api/media/files/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/{id}").permitAll()

                        // Admin endpoints - require ADMIN role
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/debug/**").hasRole("ADMIN")

                        // Post management - require authentication for write operations
                        .requestMatchers(HttpMethod.POST, "/api/posts").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/upvote").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/downvote").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/vote").hasAnyRole("USER", "ADMIN")

                        // Comment management - require authentication for write operations
                        .requestMatchers(HttpMethod.POST, "/api/comments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/comments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/**").hasAnyRole("USER", "ADMIN")
                        // Also protect implemented path variant
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/comments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/posts/*/comments/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/*/comments/**").hasAnyRole("USER", "ADMIN")

                        // Market management - require authentication for write operations
                        .requestMatchers(HttpMethod.POST, "/api/market/products").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/market/products/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/market/products/**").hasAnyRole("USER", "ADMIN")

                        // Authenticated user endpoints - require authentication
                        .requestMatchers("/api/user/**").authenticated()
                        .requestMatchers("/api/cart/**").hasAnyRole("USER", "ADMIN")
                        // Apply the same rule to both conversation paths
                        .requestMatchers("/api/conversations/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/chat/conversations/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/messages/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/media/upload").hasAnyRole("USER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(userDetailsService)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }
}