package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.out.security.JwtAuthenticationFilter;
import br.com.morbus.regulacao.adapters.out.security.JwtService;
import br.com.morbus.regulacao.adapters.out.security.SecurityConfig;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.enums.EDecisaoRegulador;
import br.com.morbus.regulacao.domain.exception.CampoObrigatorioException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.model.Parecer;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.IAvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.IReclassificarRiscoUseCase;
import br.com.morbus.regulacao.ports.in.dto.AvaliarSolicitacaoResult;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegulacaoController.class)
@Import(SecurityConfig.class)
@DisplayName("RegulacaoController")
class RegulacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean IListarSolicitacoesUseCase listarUseCase;
    @MockitoBean IAvaliarSolicitacaoUseCase avaliarUseCase;
    @MockitoBean IReclassificarRiscoUseCase reclassificarUseCase;
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

    private Solicitacao buildSolicitacao() {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), EStatusSolicitacao.APROVADA,
                ERiscoSolicitado.AMARELO, "I10", "Hipertensao grave", "Dr. Silva",
                null, EDestino.FILA_REGULADA, null, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    private AvaliarSolicitacaoResult buildAvaliarResult(EDecisaoRegulador decisao, EStatusSolicitacao novoStatus) {
        Parecer parecer = new Parecer(UUID.randomUUID(), UUID.randomUUID(), decisao, "justificativa");
        return new AvaliarSolicitacaoResult(parecer, novoStatus);
    }

    private static final String BASE_URL = "/api/v1/regulacao";

    // ── GET /api/v1/regulacao/pendentes ──────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/regulacao/pendentes")
    class Pendentes {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR lista")
        void deveRetornar200() throws Exception {
            when(listarUseCase.execute(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL + "/pendentes")
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando SOLICITANTE tenta listar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/pendentes")
                            .with(authentication(principalToken("ROLE_SOLICITANTE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /api/v1/regulacao/pendentes-vaga ─────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/regulacao/pendentes-vaga")
    class PendentesVaga {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR lista")
        void deveRetornar200() throws Exception {
            when(listarUseCase.execute(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL + "/pendentes-vaga")
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta listar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/pendentes-vaga")
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POST /api/v1/regulacao/{id}/avaliar ──────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/regulacao/{id}/avaliar")
    class Avaliar {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR autoriza")
        void deveRetornar200Autorizar() throws Exception {
            when(avaliarUseCase.execute(any()))
                    .thenReturn(buildAvaliarResult(EDecisaoRegulador.AUTORIZAR, EStatusSolicitacao.APROVADA));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "decisao": "AUTORIZAR", "riskColorDefinido": "AMARELO" }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.novoStatus").value("APROVADA"));
        }

        @Test
        @DisplayName("deve retornar 403 quando SOLICITANTE tenta avaliar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "decisao": "AUTORIZAR", "riskColorDefinido": "AMARELO" }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando decisao esta ausente")
        void deveRetornar400SemDecisao() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando riskColorDefinido esta ausente para AUTORIZAR")
        void deveRetornar400SemRiskColor() throws Exception {
            when(avaliarUseCase.execute(any()))
                    .thenThrow(new CampoObrigatorioException("riskColorDefinido e obrigatorio"));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "decisao": "AUTORIZAR" }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao nao existe")
        void deveRetornar404() throws Exception {
            when(avaliarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "decisao": "NEGAR", "justificativa": "motivo" }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando solicitacao nao esta em status permitido")
        void deveRetornar422() throws Exception {
            when(avaliarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoPendenteException("status invalido"));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/avaliar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "decisao": "NEGAR", "justificativa": "motivo" }
                                    """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ── PATCH /api/v1/regulacao/solicitacoes/{id}/risco ──────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/regulacao/solicitacoes/{id}/risco")
    class ReclassificarRisco {

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR reclassifica")
        void deveRetornar200() throws Exception {
            when(reclassificarUseCase.execute(any())).thenReturn(buildSolicitacao());

            mockMvc.perform(patch(BASE_URL + "/solicitacoes/" + UUID.randomUUID() + "/risco")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "riskColor": "VERMELHO" }
                                    """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando SOLICITANTE tenta reclassificar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/solicitacoes/" + UUID.randomUUID() + "/risco")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "riskColor": "VERMELHO" }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando riskColor esta ausente")
        void deveRetornar400() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/solicitacoes/" + UUID.randomUUID() + "/risco")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao nao existe")
        void deveRetornar404() throws Exception {
            when(reclassificarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            mockMvc.perform(patch(BASE_URL + "/solicitacoes/" + UUID.randomUUID() + "/risco")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "riskColor": "VERMELHO" }
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 422 quando status nao e APROVADA")
        void deveRetornar422() throws Exception {
            when(reclassificarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoPendenteException("status invalido"));

            mockMvc.perform(patch(BASE_URL + "/solicitacoes/" + UUID.randomUUID() + "/risco")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "riskColor": "VERMELHO" }
                                    """))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
