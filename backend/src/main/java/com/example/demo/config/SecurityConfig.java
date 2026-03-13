package com.example.demo.config;

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
            UserDetailsService userDetailsService
    ) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(jwtService, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth

                        // allow error dispatch
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()

                        // PUBLIC endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/users/municipality").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // ADMIN + MUNICIPALITY
                        .requestMatchers("/api/zones/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                   
                        .requestMatchers("/api/telemetry/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/alerts/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/anomalies/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/kpi/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.GET, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.POST, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.PUT, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers(HttpMethod.DELETE, "/api/bins/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/api/public-reports").permitAll()
                        .requestMatchers("/api/municipality/**").hasAnyRole("ADMIN", "MUNICIPALITY")
                        .requestMatchers("/uploads/**").permitAll()


                        // MISSIONS
                        .requestMatchers("/api/missions/**")
                        .hasAnyRole("ADMIN", "MUNICIPALITY", "DRIVER")
                        
                        .requestMatchers("/api/truck-locations").permitAll()
                        
                        
                        
                        .requestMatchers("/ws/**").permitAll()

                        // DRIVERS
                        .requestMatchers("/api/drivers/**")
                        .hasAnyRole("ADMIN", "MUNICIPALITY")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ✅ CORS FIXED FOR ANGULAR
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow any localhost port (4200, 5173, etc.)
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));

        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        config.setExposedHeaders(List.of("Authorization"));

        // JWT ما يحتاجش cookies
        config.setAllowCredentials(false);

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