package br.com.morbus.regulacao.adapters.in.rest;

import br.com.morbus.regulacao.adapters.out.security.JwtAuthenticationFilter;
import br.com.morbus.regulacao.adapters.out.security.JwtService;
import br.com.morbus.regulacao.adapters.out.security.SecurityConfig;
import br.com.morbus.regulacao.adapters.security.UserPrincipal;
import br.com.morbus.regulacao.domain.exception.UnidadeSolicitanteNaoEncontradaException;
import br.com.morbus.regulacao.domain.model.UnidadeSolicitante;
import br.com.morbus.regulacao.ports.in.IBuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.ICadastrarUnidadeSolicitanteUseCase;
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

@WebMvcTest(UnidadeSolicitanteController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UnidadeSolicitanteController")
class UnidadeSolicitanteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean ICadastrarUnidadeSolicitanteUseCase cadastrarUnidadeSolicitanteUseCase;
    @MockitoBean IBuscarUnidadeSolicitanteUseCase buscarUnidadeSolicitanteUseCase;
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

    private static final String BASE_URL = "/api/v1/unidades-solicitantes";

    // ── POST /api/v1/unidades-solicitantes ───────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/unidades-solicitantes")
    class Cadastrar {

        @Test
        @DisplayName("deve retornar 201 quando REGULADOR cadastra unidade valida")
        void deveRetornar201() throws Exception {
            UnidadeSolicitante unidade = new UnidadeSolicitante("1234567", "UBS Central", "Rua A, 1", "1111-1111");
            when(cadastrarUnidadeSolicitanteUseCase.execute(any())).thenReturn(unidade);

            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "cnes": "1234567", "nome": "UBS Central", "endereco": "Rua A, 1", "telefone": "1111-1111" }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.cnes").value("1234567"))
                    .andExpect(jsonPath("$.nome").value("UBS Central"));
        }

        @Test
        @DisplayName("deve retornar 403 quando SOLICITANTE tenta cadastrar unidade")
        void deveRetornar403() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_SOLICITANTE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "cnes": "1234567", "nome": "UBS Central" }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("deve retornar 400 quando cnes esta ausente")
        void deveRetornar400QuandoCnesAusente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "nome": "UBS Central" }
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("deve retornar 400 quando nome esta ausente")
        void deveRetornar400QuandoNomeAusente() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(authentication(principalToken("ROLE_REGULADOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    { "cnes": "1234567" }
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── GET /api/v1/unidades-solicitantes/{id} ───────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/unidades-solicitantes/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("deve retornar 200 quando a unidade existe")
        void deveRetornar200() throws Exception {
            UUID id = UUID.randomUUID();
            UnidadeSolicitante unidade = new UnidadeSolicitante(id, "1234567", "UBS Central", "Rua A, 1", "1111-1111");
            when(buscarUnidadeSolicitanteUseCase.execute(id)).thenReturn(unidade);

            mockMvc.perform(get(BASE_URL + "/" + id)
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        @DisplayName("deve retornar 404 quando a unidade nao existe")
        void deveRetornar404QuandoNaoExiste() throws Exception {
            UUID id = UUID.randomUUID();
            when(buscarUnidadeSolicitanteUseCase.execute(id))
                    .thenThrow(new UnidadeSolicitanteNaoEncontradaException("nao encontrada"));

            mockMvc.perform(get(BASE_URL + "/" + id)
                            .with(authentication(principalToken("ROLE_REGULADOR"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("deve retornar 403 quando MEDICO tenta consultar")
        void deveRetornar403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                            .with(authentication(principalToken("ROLE_MEDICO"))))
                    .andExpect(status().isForbidden());
        }
    }
}