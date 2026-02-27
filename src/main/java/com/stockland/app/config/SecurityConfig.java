package com.stockland.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/listings", "/properties", "/property/**", "/css/**", "/js/**", "/images/**", "/error/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
                        .requestMatchers(HttpMethod.POST, "/properties/delete/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/properties/create").authenticated()
                        .requestMatchers(HttpMethod.GET, "/properties/edit/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/properties/edit/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/properties/approve/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/properties/reject/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/properties/feature/**").hasRole("ADMIN")
                        // everything else needs login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard?login", true)
                        .failureUrl("/login?error")
                        .failureHandler(customAuthenticationFailureHandler())
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(customAccessDeniedHandler())
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("stockland-remember-me-key")
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(604800) // 7 days
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Custom login failure
    @Bean
    public AuthenticationFailureHandler customAuthenticationFailureHandler() {
        return (request, response, exception) -> {
            request.getSession().setAttribute("error", "Invalid username/email or password");
            response.sendRedirect("/login?error");
        };
    }

    // Custom access denied handler
    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            request.setAttribute("jakarta.servlet.error.status_code", 403);
            request.setAttribute("errorMessage", "You do not have permission to access this page.");
            request.getRequestDispatcher("/error").forward(request, response);
        };
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            String path = request.getRequestURI();

            if (pathExists(path)) {
                request.setAttribute("jakarta.servlet.error.status_code", 401);
                request.setAttribute("errorMessage", "You need to log in to access this page.");
                request.getRequestDispatcher("/error").forward(request, response);
            } else {
                request.setAttribute("jakarta.servlet.error.status_code", 404);
                request.setAttribute("errorMessage", "Sorry, the page you are looking for does not exist.");
                request.getRequestDispatcher("/error").forward(request, response);
            }
        };
    }

    private boolean pathExists(String path) {
        return path.startsWith("/dashboard") || path.startsWith("/create-listing")
                || path.startsWith("/settings") || path.startsWith("/favorites")
                || path.startsWith("/properties/create");
    }
}