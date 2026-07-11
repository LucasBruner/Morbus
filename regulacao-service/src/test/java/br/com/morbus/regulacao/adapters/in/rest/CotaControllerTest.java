package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.out.security.JwtAuthenticationFilter;
import br.com.morbus.regulacao.adapters.out.security.JwtService;
import br.com.morbus.regulacao.adapters.out.security.SecurityConfig;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.dto.PageResult;
import br.com.morbus.regulacao.domain.model.Quota;
import br.com.morbus.regulacao.ports.in.IConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CotaController.class)
@Import(SecurityConfig.class)
@DisplayName("CotaController")
class CotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean IConsultarCotasUseCase consultarCotasUseCase;
    @MockitoBean IGerenciarCotaUseCase gerenciarCotaUseCase;
    @MockitoBean JwtService jwtService;
    @MockitoBean JwtAuthenticationFilter jwtAuthFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            inv.getArgument(2, FilterChain.class)
               .doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken principalToken(String role) {
        UserPrincipal p = new UserPrincipal(UUID.randomUUID().toString(),
                UUID.randomUUID(), UUID.randomUUID(), role);
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private Quota buildQuota(int maxPerPeriod, int currentCount) {
        return new Quota(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                maxPerPeriod, currentCount, LocalDate.of(2026, 7, 1));
    }

    private static final String BASE_URL = "/api/v1/regulacao/cotas";

    // ── POST /api/v1/regulacao/cotas ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/regulacao/cotas")
    class Upsert {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR cria/atualiza cota valida")
        void deveRetornar200() throws Exception {
            Quota quota = buildQuota(10, 3);
            when(gerenciarCotaUseCase.execute(any())).thenReturn(quota);

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "unitId": "%s", "procedureId": "%s", "maxPerPeriod": 10, "periodStart": "2026-07-01" }
                                    """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxPerPeriod").value(10))
                    .andExpect(jsonPath("$.currentCount").value(3));
        }

        @Test
        @DisplayName("deve retornar 403 quando SOLICITANTE tenta gerenciar cota")
        void deveRetornar403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "unitId": "%s", "procedureId": "%s", "maxPerPeriod": 10, "periodStart": "2026-07-01" }
                                    """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando maxPerPeriod e menor que 1")
        void deveRetornar400QuandoMaxPerPeriodInvalido() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "unitId": "%s", "procedureId": "%s", "maxPerPeriod": 0, "periodStart": "2026-07-01" }
                                    """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando unitId esta ausente")
        void deveRetornar400QuandoUnitIdAusente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "procedureId": "%s", "maxPerPeriod": 10, "periodStart": "2026-07-01" }
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando periodStart esta ausente")
        void deveRetornar400QuandoPeriodStartAusente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "unitId": "%s", "procedureId": "%s", "maxPerPeriod": 10 }
                                    """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/v1/regulacao/cotas ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/regulacao/cotas")
    class Listar {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR consulta sem filtros")
        void deveRetornar200SemFiltros() throws Exception {
            when(consultarCotasUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta consultar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve aceitar filtros unitId, procedureId e mes")
        void deveAceitarFiltros() throws Exception {
            when(consultarCotasUseCase.execute(any())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

            mockMvc.perform(get(BASE_URL)
                            .param("unitId", UUID.randomUUID().toString())
                            .param("procedureId", UUID.randomUUID().toString())
                            .param("mes", "2026-06")
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 400 quando mes esta em formato invalido")
        void deveRetornar400QuandoMesInvalido() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("mes", "junho-2026")
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar a lista de cotas devolvida pelo use case")
        void deveRetornarListaDeCotas() throws Exception {
            Quota quota = buildQuota(10, 4);
            when(consultarCotasUseCase.execute(any())).thenReturn(new PageResult<>(List.of(quota), 0, 20, 1, 1));

            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].maxPerPeriod").value(10))
                    .andExpect(jsonPath("$.content[0].currentCount").value(4));
        }
    }
}