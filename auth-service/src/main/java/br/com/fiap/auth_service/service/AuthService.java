package br.com.fiap.auth_service.service;

import br.com.fiap.auth_service.exception.PasswordNotValidException;
import br.com.fiap.auth_service.exception.UserAlreadyExistException;
import br.com.fiap.auth_service.model.User;
import br.com.fiap.auth_service.model.dto.NewUserDTO;
import br.com.fiap.auth_service.repository.UserRepository;
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
}
