package br.com.morbus.agendamento.adapter.out.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    @Profile("local")
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        return configureBaseSecurity(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/graphiql", "/graphiql/**").permitAll()
                        .requestMatchers("/graphql", "/graphql/**").authenticated()
                        .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    @Profile("!local")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return configureBaseSecurity(http)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/graphql", "/graphql/**").authenticated()
                        .anyRequest().authenticated()
                )
                .build();
    }

    private HttpSecurity configureBaseSecurity(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeProblemDetail(response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "https://morbus.sus.gov.br/problems/invalid-credentials",
                                        "Nao autorizado",
                                        "Token JWT ausente ou invalido."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeProblemDetail(response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "https://morbus.sus.gov.br/problems/access-denied",
                                        "Acesso negado",
                                        "Seu perfil nao tem permissao para esta operacao."))
                );
    }

    private void writeProblemDetail(HttpServletResponse response,
                                    int status,
                                    String type,
                                    String title,
                                    String detail) throws java.io.IOException {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.valueOf(status));
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setDetail(detail);

        response.setStatus(status);
        response.setContentType("application/problem+json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
