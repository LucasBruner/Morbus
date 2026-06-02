package br.com.morbus.authservice.controller;

import br.com.morbus.authservice.config.SecurityConfig;
import br.com.morbus.authservice.exception.PasswordNotValidException;
import br.com.morbus.authservice.exception.UserAlreadyExistException;
import br.com.morbus.authservice.exception.UserOrPasswordIncorrect;
import br.com.morbus.authservice.model.User;
import br.com.morbus.authservice.model.UserRole;
import br.com.morbus.authservice.service.AuthService;
import br.com.morbus.authservice.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL    = "/auth/login";

    private User buildUser(String username, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@sus.gov.br");
        user.setPassword("$2b$10$hash");
        user.setRole(role);
        return user;
    }

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("deve retornar 201 com dados do usuário criado")
        void success() throws Exception {
            User saved = buildUser("dr_silva", UserRole.MEDICO);
            when(authService.createNewUser(any())).thenReturn(saved);

            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "email": "silva@sus.gov.br",
                                      "password": "Medico@123",
                                      "role": "MEDICO"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("dr_silva"))
                    .andExpect(jsonPath("$.role").value("MEDICO"));
        }

        @Test
        @DisplayName("deve retornar 400 quando username está em branco")
        void blankUsername() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "",
                                      "email": "silva@sus.gov.br",
                                      "password": "Medico@123",
                                      "role": "MEDICO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("username"));
        }

        @Test
        @DisplayName("deve retornar 400 quando e-mail é inválido")
        void invalidEmail() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "email": "nao-e-email",
                                      "password": "Medico@123",
                                      "role": "MEDICO"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("email"));
        }

        @Test
        @DisplayName("deve retornar 400 quando role é inválida")
        void invalidRole() throws Exception {
            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "email": "silva@sus.gov.br",
                                      "password": "Medico@123",
                                      "role": "USER"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("deve retornar 409 quando username já existe")
        void duplicateUsername() throws Exception {
            when(authService.createNewUser(any()))
                    .thenThrow(new UserAlreadyExistException("Username 'dr_silva' já está em uso"));

            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "email": "silva@sus.gov.br",
                                      "password": "Medico@123",
                                      "role": "MEDICO"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Username 'dr_silva' já está em uso"));
        }

        @Test
        @DisplayName("deve retornar 422 quando senha não atende aos requisitos")
        void weakPassword() throws Exception {
            when(authService.createNewUser(any()))
                    .thenThrow(new PasswordNotValidException(
                            "Senha deve ter no mínimo 9 caracteres, incluindo letra maiúscula, minúscula, número e caractere especial"));

            mockMvc.perform(post(REGISTER_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "email": "silva@sus.gov.br",
                                      "password": "fraca",
                                      "role": "MEDICO"
                                    }
                                    """))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.title").value("Senha inválida"));
        }
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("deve retornar 200 com token quando credenciais são válidas")
        void success() throws Exception {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            when(authService.doLogin(any())).thenReturn(user);
            when(jwtService.generateToken(user)).thenReturn("eyJhbGci.payload.signature");
            when(jwtService.getExpirationMs()).thenReturn(86_400_000L);

            mockMvc.perform(post(LOGIN_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "password": "Medico@123"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("eyJhbGci.payload.signature"))
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(86_400_000))
                    .andExpect(jsonPath("$.role").value("MEDICO"));
        }

        @Test
        @DisplayName("deve retornar 401 quando credenciais são inválidas")
        void invalidCredentials() throws Exception {
            when(authService.doLogin(any()))
                    .thenThrow(new UserOrPasswordIncorrect("Usuário ou senha incorretos!"));

            mockMvc.perform(post(LOGIN_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "dr_silva",
                                      "password": "SenhaErrada@1"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Credenciais inválidas"));
        }

        @Test
        @DisplayName("deve retornar 400 quando username está em branco")
        void blankUsername() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "",
                                      "password": "Medico@123"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations[0].field").value("username"));
        }

        @Test
        @DisplayName("deve retornar 400 quando body está ausente")
        void missingBody() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
