package com.example.demo.config;

import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtAuthFilter;
import com.example.demo.security.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserRepository userRepository
    ) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userDetailsService, userRepository);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/register-driver").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password-by-code").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/resend-verification").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/verify-reset-code").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/approve-driver").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/reject-driver").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/truck-locations/**").permitAll()
                        .requestMatchers("/api/truck-locations").permitAll()
                        .requestMatchers("/api/telemetry/**").permitAll()
                        .requestMatchers("/api/public-reports").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/users/municipality").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users/maintenance").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/security/**").hasRole("ADMIN")

                        .requestMatchers("/api/zones/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/municipality/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/alerts/**").hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER")
                        .requestMatchers("/api/anomalies/**").hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER", "MAINTENANCE")
                        .requestMatchers("/api/kpi/**").hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER")

                        .requestMatchers(HttpMethod.GET, "/api/bins/*/qrcode").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.GET, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER", "MAINTENANCE")
                        .requestMatchers(HttpMethod.POST, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.PUT, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY", "MAINTENANCE")
                        .requestMatchers(HttpMethod.DELETE, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")

                        .requestMatchers("/api/missions/**").hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER")
                        .requestMatchers("/api/trucks/**").hasAnyRole("ADMIN", "MUNICIPALITY", "MAINTENANCE")
                        .requestMatchers("/api/truck-incidents/**").hasAnyRole("ADMIN", "MUNICIPALITY", "MAINTENANCE")

                        .requestMatchers("/api/drivers/*/my-bins").hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers("/api/drivers/me/truck").hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/drivers/bin-scan").hasRole("DRIVER")
                        .requestMatchers("/api/drivers/**").hasAnyRole("ADMIN", "MUNICIPALITY")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}