package br.com.morbus.authservice.service;

import br.com.morbus.authservice.exception.PasswordNotValidException;
import br.com.morbus.authservice.exception.UserAlreadyExistException;
import br.com.morbus.authservice.exception.UserOrPasswordIncorrect;
import br.com.morbus.authservice.model.User;
import br.com.morbus.authservice.model.UserRole;
import br.com.morbus.authservice.model.dto.LoginRequestDTO;
import br.com.morbus.authservice.model.dto.NewUserDTO;
import br.com.morbus.authservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private static final String VALID_PASSWORD = "Senha@Forte1";
    private static final String WEAK_PASSWORD  = "fraca";

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private User buildUser(String username, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@sus.gov.br");
        user.setPassword("$2b$10$hashedpassword");
        user.setRole(role);
        return user;
    }

    // ── createNewUser ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNewUser")
    class CreateNewUser {

        @Test
        @DisplayName("deve criar usuário com sucesso quando dados são válidos")
        void success() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", VALID_PASSWORD, UserRole.MEDICO);

            when(userRepository.existsByUsername("dr_silva")).thenReturn(false);
            when(userRepository.existsByEmail("silva@sus.gov.br")).thenReturn(false);
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("$2b$10$hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.createNewUser(dto);

            assertThat(result.getUsername()).isEqualTo("dr_silva");
            assertThat(result.getRole()).isEqualTo(UserRole.MEDICO);
            assertThat(result.getPassword()).isEqualTo("$2b$10$hash");
            assertThat(result.getUnitId()).isNull();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("deve propagar unitId para EXECUTANTE")
        void propagatesUnitIdForExecutante() {
            UUID unitId = UUID.randomUUID();
            NewUserDTO dto = new NewUserDTO("executante_1", "executante@sus.gov.br", VALID_PASSWORD, UserRole.EXECUTANTE, unitId);

            when(userRepository.existsByUsername("executante_1")).thenReturn(false);
            when(userRepository.existsByEmail("executante@sus.gov.br")).thenReturn(false);
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("$2b$10$hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = authService.createNewUser(dto);

            assertThat(result.getUnitId()).isEqualTo(unitId);
        }

        @Test
        @DisplayName("deve lançar exceção quando username já existe")
        void usernameAlreadyExists() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", VALID_PASSWORD, UserRole.MEDICO);

            when(userRepository.existsByUsername("dr_silva")).thenReturn(true);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(UserAlreadyExistException.class)
                    .hasMessageContaining("dr_silva");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando e-mail já existe")
        void emailAlreadyExists() {
            NewUserDTO dto = new NewUserDTO("novo_user", "silva@sus.gov.br", VALID_PASSWORD, UserRole.PACIENTE);

            when(userRepository.existsByUsername("novo_user")).thenReturn(false);
            when(userRepository.existsByEmail("silva@sus.gov.br")).thenReturn(true);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(UserAlreadyExistException.class)
                    .hasMessageContaining("silva@sus.gov.br");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção quando senha é fraca (sem maiúscula)")
        void weakPassword_noUppercase() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", "senha@fraca1", UserRole.MEDICO);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(PasswordNotValidException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando senha é fraca (sem caractere especial)")
        void weakPassword_noSpecialChar() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", "SenhaFraca1", UserRole.MEDICO);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(PasswordNotValidException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando senha é muito curta")
        void weakPassword_tooShort() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", "Ab@1", UserRole.MEDICO);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(PasswordNotValidException.class)
                    .hasMessageContaining("9 caracteres");
        }

        @Test
        @DisplayName("não deve chamar passwordEncoder se validação falhar")
        void doesNotEncodeOnValidationFailure() {
            NewUserDTO dto = new NewUserDTO("dr_silva", "silva@sus.gov.br", WEAK_PASSWORD, UserRole.MEDICO);

            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.createNewUser(dto))
                    .isInstanceOf(PasswordNotValidException.class);

            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ── doLogin ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("doLogin")
    class DoLogin {

        @Test
        @DisplayName("deve retornar usuário quando credenciais são válidas")
        void success() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            LoginRequestDTO dto = new LoginRequestDTO("dr_silva", "Senha@Forte1");

            when(userRepository.findByUsername("dr_silva")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Senha@Forte1", user.getPassword())).thenReturn(true);

            User result = authService.doLogin(dto);

            assertThat(result.getUsername()).isEqualTo("dr_silva");
            assertThat(result.getRole()).isEqualTo(UserRole.MEDICO);
        }

        @Test
        @DisplayName("deve lançar exceção quando usuário não existe")
        void userNotFound() {
            LoginRequestDTO dto = new LoginRequestDTO("nao_existe", "Senha@Forte1");

            when(userRepository.findByUsername("nao_existe")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.doLogin(dto))
                    .isInstanceOf(UserOrPasswordIncorrect.class)
                    .hasMessageContaining("incorretos");
        }

        @Test
        @DisplayName("deve lançar exceção quando senha está errada")
        void wrongPassword() {
            User user = buildUser("dr_silva", UserRole.MEDICO);
            LoginRequestDTO dto = new LoginRequestDTO("dr_silva", "SenhaErrada@1");

            when(userRepository.findByUsername("dr_silva")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("SenhaErrada@1", user.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> authService.doLogin(dto))
                    .isInstanceOf(UserOrPasswordIncorrect.class)
                    .hasMessageContaining("incorretos");
        }

        @Test
        @DisplayName("mensagem de erro deve ser genérica para usuário e senha (não revelar qual está errado)")
        void errorMessageIsGeneric() {
            LoginRequestDTO dto = new LoginRequestDTO("nao_existe", "qualquer");

            when(userRepository.findByUsername("nao_existe")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.doLogin(dto))
                    .isInstanceOf(UserOrPasswordIncorrect.class)
                    .hasMessage("Usuário ou senha incorretos!");
        }
    }
}
