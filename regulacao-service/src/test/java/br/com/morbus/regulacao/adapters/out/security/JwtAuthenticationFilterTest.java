package br.com.morbus.regulacao.adapters.out.security;

import br.com.morbus.regulacao.adapters.security.UserPrincipal;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ── Sem header ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando Authorization header está ausente")
    class SemHeader {

        @Test
        @DisplayName("deve passar para o próximo filtro sem autenticar")
        void devePassarSemAutenticar() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("não deve chamar jwtService")
        void naoDeveChamarJwtService() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
            verify(jwtService, never()).validateToken(org.mockito.ArgumentMatchers.any());
        }
    }

    // ── Header sem "Bearer " ──────────────────────────────────────────────────

    @Nested
    @DisplayName("quando Authorization header não começa com Bearer")
    class SemBearer {

        @Test
        @DisplayName("deve passar para o próximo filtro sem autenticar")
        void devePassarSemAutenticar() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    // ── Token inválido ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando token é inválido")
    class TokenInvalido {

        @Test
        @DisplayName("deve passar para o próximo filtro sem autenticar")
        void devePassarSemAutenticar() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer token.invalido");
            when(jwtService.validateToken("token.invalido")).thenReturn(false);
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    // ── Token válido ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quando token é válido")
    class TokenValido {

        @Test
        @DisplayName("deve autenticar com UserPrincipal correto no SecurityContext")
        void deveAutenticar() throws Exception {
            String userId = UUID.randomUUID().toString();
            String unitId = UUID.randomUUID().toString();
            String token = "token.valido.aqui";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn(userId);
            when(jwtService.extractRole(token)).thenReturn("MEDICO");
            when(jwtService.extractUnitId(token)).thenReturn(unitId);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

            UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
            assertThat(principal.username()).isEqualTo(userId);
            assertThat(principal.role()).isEqualTo("ROLE_MEDICO");
            assertThat(principal.unitId()).isEqualTo(UUID.fromString(unitId));
        }

        @Test
        @DisplayName("deve adicionar prefixo ROLE_ quando role não tem")
        void deveAdicionarPrefixoRole() throws Exception {
            String token = "token.valido.aqui";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn(UUID.randomUUID().toString());
            when(jwtService.extractRole(token)).thenReturn("PACIENTE");
            when(jwtService.extractUnitId(token)).thenReturn(null);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            assertThat(principal.role()).isEqualTo("ROLE_PACIENTE");
        }

        @Test
        @DisplayName("não deve duplicar prefixo ROLE_ quando já está presente")
        void naoDeveDuplicarPrefixo() throws Exception {
            String token = "token.valido.aqui";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + token);

            when(jwtService.validateToken(token)).thenReturn(true);
            when(jwtService.extractUsername(token)).thenReturn(UUID.randomUUID().toString());
            when(jwtService.extractRole(token)).thenReturn("ROLE_SOLICITANTE");
            when(jwtService.extractUnitId(token)).thenReturn(null);

            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

            UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
            assertThat(principal.role()).isEqualTo("ROLE_SOLICITANTE");
        }
    }
}
