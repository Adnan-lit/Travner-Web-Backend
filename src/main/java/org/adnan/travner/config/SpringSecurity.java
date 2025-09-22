package org.adnan.travner.config;

import org.adnan.travner.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SpringSecurity {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/posts").permitAll()
                        .requestMatchers("/posts/search").permitAll()
                        .requestMatchers("/posts/location").permitAll()
                        .requestMatchers("/posts/tags").permitAll()
                        .requestMatchers("/posts/{id}").permitAll()
                        .requestMatchers("/posts/{postId}/comments").permitAll()
                        .requestMatchers("/posts/{postId}/comments/{id}").permitAll()
                        .requestMatchers("/posts/{postId}/media/{mediaId}").permitAll() // Allow media file access
                        .requestMatchers("/actuator/health").permitAll() // Allow health checks
                        .requestMatchers("/debug/**").hasRole("ADMIN") // Secure debug endpoints
                        .requestMatchers("/api/auth/**").authenticated() // Auth endpoints require authentication
                        .requestMatchers("/api/chat/**").authenticated() // Chat endpoints require authentication
                        .requestMatchers("/user/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }
}