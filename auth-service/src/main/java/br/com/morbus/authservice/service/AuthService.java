package br.com.morbus.authservice.service;

import br.com.morbus.authservice.exception.PasswordNotValidException;
import br.com.morbus.authservice.exception.UserAlreadyExistException;
import br.com.morbus.authservice.exception.UserOrPasswordIncorrect;
import br.com.morbus.authservice.model.User;
import br.com.morbus.authservice.model.dto.LoginRequestDTO;
import br.com.morbus.authservice.model.dto.NewUserDTO;
import br.com.morbus.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\W)(?=.*\\d).{9,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createNewUser(NewUserDTO newUserDTO) {
        if (userRepository.existsByUsername(newUserDTO.username())) {
            throw new UserAlreadyExistException("Username '" + newUserDTO.username() + "' já está em uso");
        }
        if (userRepository.existsByEmail(newUserDTO.email())) {
            throw new UserAlreadyExistException("E-mail '" + newUserDTO.email() + "' já está cadastrado");
        }

        validatePassword(newUserDTO.password());

        User newUser = new User();
        newUser.setUsername(newUserDTO.username());
        newUser.setEmail(newUserDTO.email());
        newUser.setPassword(passwordEncoder.encode(newUserDTO.password()));
        newUser.setRole(newUserDTO.role());
        newUser.setUnitId(newUserDTO.unitId());

        return userRepository.save(newUser);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new PasswordNotValidException("Senha é obrigatória");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new PasswordNotValidException(
                    "Senha deve ter no mínimo 9 caracteres, incluindo letra maiúscula, minúscula, número e caractere especial");
        }
    }

    public User doLogin(LoginRequestDTO loginRequestDTO) {
        User user = userRepository
                .findByUsername(loginRequestDTO.username())
                .orElseThrow(() -> new UserOrPasswordIncorrect("Usuário ou senha incorretos!"));

        if (!passwordEncoder.matches(loginRequestDTO.password(), user.getPassword())) {
            throw new UserOrPasswordIncorrect("Usuário ou senha incorretos!");
        }

        return user;
    }
}
