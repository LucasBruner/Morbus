package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.adapter.in.rest.dto.AgendamentoCreatedResponseDTO;
import br.com.morbus.agendamento.adapter.in.rest.dto.AgendamentoRequestDTO;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agendamentos")
public class AgendamentoController {

    private final ICriarAgendamentoUseCase criarAgendamentoUseCase;

    public AgendamentoController(ICriarAgendamentoUseCase criarAgendamentoUseCase) {
        this.criarAgendamentoUseCase = criarAgendamentoUseCase;
    }

    @PostMapping
    public ResponseEntity<AgendamentoCreatedResponseDTO> criar (
            @Valid @RequestBody AgendamentoRequestDTO request) {
        Agendamento agendamento = criarAgendamentoUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AgendamentoCreatedResponseDTO.fromDomain(agendamento));
    }
}
