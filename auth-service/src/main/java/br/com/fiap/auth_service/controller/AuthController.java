package br.com.fiap.auth_service.controller;

import br.com.fiap.auth_service.model.User;
import br.com.fiap.auth_service.model.dto.NewUserDTO;
import br.com.fiap.auth_service.model.dto.UserPresenterDTO;
import br.com.fiap.auth_service.service.AuthService;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserPresenterDTO> registerUser(@Valid @RequestBody NewUserDTO newUserDTO) {
        User user = authService.createNewUser(newUserDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserPresenterDTO(user.getId().toString(), user.getUsername(), user.getRole().toString()));
    }

}
