package com.buymore;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean HttpSessionSecurityContextRepository contexts() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a
            .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login", "/api/health", "/api/contact", "/error").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()
            .requestMatchers("/api/auth/**").authenticated()
            .requestMatchers("/api/account/**").authenticated()
            .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/orders/*").authenticated()
            .requestMatchers("/api/**").hasRole("ADMIN")
            .anyRequest().denyAll())
            .securityContext(c -> c.securityContextRepository(contexts()))
            .requestCache(c -> c.disable())
            .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> {res.setStatus(401); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Please sign in to continue.\"}");})
                .accessDeniedHandler((req,res,ex) -> {res.setStatus(403); res.setContentType("application/json"); res.getWriter().write("{\"message\":\"Access denied or session token expired. Refresh and try again.\"}");}))
            .build(); // Session-backed CSRF protection remains enabled, including login and registration.
    }
}
