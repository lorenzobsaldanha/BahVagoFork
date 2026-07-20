package com.bahvago.config;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication)
                    throws IOException, ServletException {

                boolean isHoteleiro = authentication.getAuthorities()
                        .contains(new SimpleGrantedAuthority("ROLE_HOTELEIRO"));

                response.sendRedirect(isHoteleiro ? "/dashboard" : "/");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            .authorizeHttpRequests(auth -> auth

                // Permite o despacho interno para páginas de erro (404, 500, etc.)
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Endpoint de erro do Spring Boot
                .requestMatchers("/error", "/error/**").permitAll()

                // Recursos estáticos
                .requestMatchers(
                        "/",
                        "/static/**",
                        "/css/**",
                        "/js/**",
                        "/img/**"
                ).permitAll()

                // Login e cadastro
                .requestMatchers(
                        "/login",
                        "/login-hoteleiro",
                        "/cadastro",
                        "/usuarios/cadastro"
                ).permitAll()

                // Páginas públicas
                .requestMatchers(
                        "/hoteis/**",
                        "/quartos/**"
                ).permitAll()

                // Área do hoteleiro
                .requestMatchers(
                        "/dashboard",
                        "/estatisticas",
                        "/gerenciar-hotel",
                        "/gerenciar-quartos",
                        "/gerenciar-avaliacoes"
                ).hasRole("HOTELEIRO")

                // Área autenticada
                .requestMatchers(
                        "/usuarios/perfil/**",
                        "/usuarios/atualizar/**",
                        "/usuarios/deletar/**"
                ).authenticated()

                // Todo o restante exige autenticação
                .anyRequest().permitAll()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("senha")
                .successHandler(authenticationSuccessHandler())
                .failureUrl("/login?error")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}