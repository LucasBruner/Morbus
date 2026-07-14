package br.com.morbus.agendamento.adapter.out.security;

import br.com.morbus.agendamento.adapter.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("quando token e valido")
    class TokenValido {

        @Test
        @DisplayName("deve autenticar com userId, unitId e role corretos quando username e um UUID")
        void deveAutenticarComUuidValido() throws Exception {
            String userId = UUID.randomUUID().toString();
            String unitId = UUID.randomUUID().toString();
            String token = "token.valido.aqui";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn(userId);
            when(jwtService.extractRole(token)).thenReturn("EXECUTANTE");
            when(jwtService.extractUnitId(token)).thenReturn(unitId);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            assertThat(principal.userId()).isEqualTo(UUID.fromString(userId));
            assertThat(principal.unitId()).isEqualTo(UUID.fromString(unitId));
            assertThat(principal.role()).isEqualTo("ROLE_EXECUTANTE");
        }

        @Test
        @DisplayName("deve autenticar com userId nulo quando username nao e um UUID valido, em vez de lancar excecao")
        void deveAutenticarComUserIdNuloQuandoUsernameNaoEUuid() throws Exception {
            String token = "token.valido.aqui";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn("dr.joao");
            when(jwtService.extractRole(token)).thenReturn("MEDICO");
            when(jwtService.extractUnitId(token)).thenReturn(null);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            assertThat(principal.username()).isEqualTo("dr.joao");
            assertThat(principal.userId()).isNull();
            assertThat(principal.role()).isEqualTo("ROLE_MEDICO");
        }

        @Test
        @DisplayName("nao deve autenticar quando role e nula")
        void naoDeveAutenticarQuandoRoleNula() throws Exception {
            String token = "token.valido.aqui";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn(UUID.randomUUID().toString());
            when(jwtService.extractRole(token)).thenReturn(null);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("quando token e invalido ou ausente")
    class TokenInvalidoOuAusente {

        @Test
        @DisplayName("deve passar para o proximo filtro sem autenticar quando header esta ausente")
        void semHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("deve passar para o proximo filtro sem autenticar quando token e invalido")
        void tokenInvalido() throws Exception {
            String token = "token.invalido";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);
            when(jwtService.validateToken(token)).thenReturn(false);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}
