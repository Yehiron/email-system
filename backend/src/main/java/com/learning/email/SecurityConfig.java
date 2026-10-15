package com.learning.email;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
class SecurityConfig {
    @Bean
    UserDetailsService users(@Value("${app.username}") String username, @Value("${app.password}") String password) {
        if (password.isBlank()) throw new IllegalStateException("APP_PASSWORD no puede estar vacía");
        return new InMemoryUserDetailsManager(User.withUsername(username)
            .password("{bcrypt}" + new BCryptPasswordEncoder().encode(password)).roles("SENDER").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, @Value("${app.allowed-origin}") String origin) throws Exception {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(origin));
        cors.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return http.cors(c -> c.configurationSource(source))
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .httpBasic(b -> b.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Usuario o contraseña incorrectos\"}");
            })).build();
    }
}
