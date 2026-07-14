package br.com.morbus.authservice.controller;

import br.com.morbus.authservice.model.User;
import br.com.morbus.authservice.model.dto.AuthResponseDTO;
import br.com.morbus.authservice.model.dto.LoginRequestDTO;
import br.com.morbus.authservice.model.dto.NewUserDTO;
import br.com.morbus.authservice.model.dto.UserPresenterDTO;
import br.com.morbus.authservice.service.AuthService;
import br.com.morbus.authservice.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService,
                          JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserPresenterDTO> registerUser(@Valid @RequestBody NewUserDTO newUserDTO) {
        User user = authService.createNewUser(newUserDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserPresenterDTO(
                        user.getId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().toString(),
                        user.getUnitId() != null ? user.getUnitId().toString() : null,
                        user.getCreatedAt()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        User user = authService.doLogin(loginRequestDTO);
        String token = jwtService.generateToken(user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(AuthResponseDTO.of(token, jwtService.getExpirationMs(), user.getRole()));
    }
}
