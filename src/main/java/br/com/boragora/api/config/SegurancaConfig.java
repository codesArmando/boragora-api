package br.com.boragora.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Regras de acesso por prefixo (plano, seção 2.4). A checagem por recurso fica no ControleDeAcesso. */
@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> conversorJwt,
            AuthenticationEntryPoint naoAutenticado,
            AccessDeniedHandler acessoNegado) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/**", "/health", "/error").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/aluno/**").hasRole("ALUNO")
                        .requestMatchers("/motorista/**").hasRole("MOTORISTA")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // /me, /grupos e /turmas: autenticado; a visibilidade por recurso fica no ControleDeAcesso.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversorJwt))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
