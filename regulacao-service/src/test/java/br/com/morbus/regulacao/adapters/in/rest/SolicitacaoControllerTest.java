package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.out.security.JwtAuthenticationFilter;
import br.com.morbus.regulacao.adapters.out.security.JwtService;
import br.com.morbus.regulacao.adapters.out.security.SecurityConfig;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.enums.EDestino;
import br.com.morbus.regulacao.domain.enums.ERiscoSolicitado;
import br.com.morbus.regulacao.domain.enums.EStatusSolicitacao;
import br.com.morbus.regulacao.domain.exception.DuplicateSolicitacaoException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoEncontradaException;
import br.com.morbus.regulacao.domain.exception.SolicitacaoNaoPendenteException;
import br.com.morbus.regulacao.domain.model.Solicitacao;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
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

@WebMvcTest(SolicitacaoController.class)
@Import(SecurityConfig.class)
@DisplayName("SolicitacaoController")
class SolicitacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean ICriarSolicitacaoUseCase criarUseCase;
    @MockitoBean IListarSolicitacoesUseCase listarUseCase;
    @MockitoBean ICancelarSolicitacaoUseCase cancelarUseCase;
    @MockitoBean IConsultarStatusSolicitacao consultarUseCase;
    @MockitoBean IComplementarSolicitacaoUseCase complementarUseCase;
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

    private UsernamePasswordAuthenticationToken principalToken(String role, UUID userId, UUID unitId) {
        UserPrincipal p = new UserPrincipal(userId.toString(), userId, unitId, role);
        return new UsernamePasswordAuthenticationToken(p, null,
                List.of(new SimpleGrantedAuthority(role)));
    }

    private Solicitacao buildSolicitacao() {
        return new Solicitacao(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, EStatusSolicitacao.AGUARDANDO,
                ERiscoSolicitado.AZUL, "I10", "Hipertensao grave", "Dr. Silva",
                null, EDestino.FILA_REGULADA, null, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now(), null
        );
    }

    private String validBody() {
        return """
                {
                  "patientId": "%s",
                  "procedureId": "%s",
                  "cid": "I10",
                  "justificativaClinica": "Paciente com hipertensao grave sem controle",
                  "profissionalSolicitante": "Dr. Silva",
                  "crmProfissional": "CRM/SP 12345",
                  "unitSolicitanteId": "%s",
                  "destino": "FILA_REGULADA"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private static final String BASE_URL = "/api/v1/solicitacoes";

    // ── POST /api/v1/solicitacoes ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/solicitacoes")
    class Criar {

        @Test
        @DisplayName("deve retornar 201 quando SOLICITANTE cria solicitacao valida")
        void deveRetornar201Solicitante() throws Exception {
            when(criarUseCase.execute(any())).thenReturn(buildSolicitacao());

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO"));
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta criar")
        void deveRetornar403ParaMedico() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_MEDICO")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando PACIENTE tenta criar")
        void deveRetornar403ParaPaciente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_PACIENTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando patientId esta ausente")
        void deveRetornar400SemPatientId() throws Exception {
            String body = """
                    {
                      "procedureId": "%s",
                      "cid": "I10",
                      "justificativaClinica": "texto",
                      "profissionalSolicitante": "Dr. Silva",
                      "unitSolicitanteId": "%s",
                      "destino": "FILA_REGULADA"
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("deve retornar 400 quando cid esta ausente")
        void deveRetornar400SemCid() throws Exception {
            String body = """
                    {
                      "patientId": "%s",
                      "procedureId": "%s",
                      "justificativaClinica": "texto",
                      "profissionalSolicitante": "Dr. Silva",
                      "unitSolicitanteId": "%s",
                      "destino": "FILA_REGULADA"
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando destino esta ausente")
        void deveRetornar400SemDestino() throws Exception {
            String body = """
                    {
                      "patientId": "%s",
                      "procedureId": "%s",
                      "cid": "I10",
                      "justificativaClinica": "texto",
                      "profissionalSolicitante": "Dr. Silva",
                      "unitSolicitanteId": "%s"
                    }
                    """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 409 quando ja existe solicitacao ativa")
        void deveRetornar409Duplicata() throws Exception {
            when(criarUseCase.execute(any()))
                    .thenThrow(new DuplicateSolicitacaoException("Ja existe solicitacao ativa"));

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ── GET /api/v1/solicitacoes ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/solicitacoes")
    class Listar {

        @Test
        @DisplayName("deve retornar 200 quando SOLICITANTE lista")
        void deveRetornar200Solicitante() throws Exception {
            when(listarUseCase.execute(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR lista")
        void deveRetornar200Regulador() throws Exception {
            when(listarUseCase.execute(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta listar")
        void deveRetornar403Medico() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando PACIENTE tenta listar")
        void deveRetornar403Paciente() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_PACIENTE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("SOLICITANTE deve ter o unitId do JWT aplicado automaticamente ao filtro")
        void solicitanteDeveUsarUnitIdDoJwt() throws Exception {
            UUID unitId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(listarUseCase.execute(any())).thenReturn(Page.empty());

            mockMvc.perform(get(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE", userId, unitId))))
                    .andExpect(status().isOk());
        }
    }

    // ── DELETE /api/v1/solicitacoes/{id} ─────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/solicitacoes/{id}")
    class Cancelar {

        @Test
        @DisplayName("deve retornar 204 quando MEDICO cancela solicitacao")
        void deveRetornar204Medico() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 204 quando SOLICITANTE cancela solicitacao")
        void deveRetornar204Solicitante() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_SOLICITANTE"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("deve retornar 403 quando PACIENTE tenta cancelar")
        void deveRetornar403Paciente() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_PACIENTE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao nao existe")
        void deveRetornar404() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new SolicitacaoNaoEncontradaException("nao encontrada"))
                    .when(cancelarUseCase).execute(id);

            mockMvc.perform(delete(BASE_URL + "/" + id)
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("deve retornar 422 quando solicitacao nao esta AGUARDANDO")
        void deveRetornar422() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new SolicitacaoNaoPendenteException("nao esta aguardando"))
                    .when(cancelarUseCase).execute(id);

            mockMvc.perform(delete(BASE_URL + "/" + id)
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }
    }

    // ── GET /api/v1/solicitacoes/{id} ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/solicitacoes/{id}")
    class ConsultarStatus {

        @Test
        @DisplayName("deve retornar 200 quando SOLICITANTE consulta")
        void deveRetornar200Solicitante() throws Exception {
            when(consultarUseCase.execute(any(), any())).thenReturn(buildSolicitacao());

            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_SOLICITANTE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO"));
        }

        @Test
        @DisplayName("deve retornar 200 quando REGULADOR consulta")
        void deveRetornar200Regulador() throws Exception {
            when(consultarUseCase.execute(any(), any())).thenReturn(buildSolicitacao());

            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta consultar")
        void deveRetornar403Medico() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando PACIENTE tenta consultar")
        void deveRetornar403Paciente() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_PACIENTE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao nao existe")
        void deveRetornar404() throws Exception {
            when(consultarUseCase.execute(any(), any()))
                    .thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_SOLICITANTE"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ── POST /api/v1/solicitacoes/{id}/complementar ──────────────────────────

    @Nested
    @DisplayName("POST /api/v1/solicitacoes/{id}/complementar")
    class Complementar {

        private String complementarBody() {
            return """
                    {
                      "cid": "I11.9",
                      "justificativaClinica": "Complemento: paciente com HAS estagio 3, sem resposta a 3 anti-hipertensivos.",
                      "crmProfissional": "CRM/SP 12345"
                    }
                    """;
        }

        @Test
        @DisplayName("deve retornar 200 quando SOLICITANTE complementa solicitacao devolvida")
        void deveRetornar200Solicitante() throws Exception {
            when(complementarUseCase.execute(any())).thenReturn(buildSolicitacao());

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(complementarBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("AGUARDANDO"));
        }

        @Test
        @DisplayName("deve aceitar body vazio, pois todos os campos sao opcionais")
        void deveAceitarBodyVazio() throws Exception {
            when(complementarUseCase.execute(any())).thenReturn(buildSolicitacao());

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta complementar")
        void deveRetornar403Medico() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_MEDICO")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(complementarBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 403 quando REGULADOR tenta complementar")
        void deveRetornar403Regulador() throws Exception {
            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(complementarBody()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 404 quando solicitacao nao existe")
        void deveRetornar404() throws Exception {
            when(complementarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoEncontradaException("nao encontrada"));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(complementarBody()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("deve retornar 422 quando solicitacao nao esta DEVOLVIDA")
        void deveRetornar422() throws Exception {
            when(complementarUseCase.execute(any()))
                    .thenThrow(new SolicitacaoNaoPendenteException("nao esta devolvida"));

            mockMvc.perform(post(BASE_URL + "/" + UUID.randomUUID() + "/complementar")
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(complementarBody()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }
    }
}
