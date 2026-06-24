package br.com.morbus.agendamento.adapter.in.rest;

import br.com.morbus.agendamento.application.command.CriarAgendamentoCommand;
import br.com.morbus.agendamento.domain.model.Agendamento;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final ICriarAgendamentoUseCase criarAgendamentoUseCase;

    public AgendamentoController(ICriarAgendamentoUseCase criarAgendamentoUseCase) {
        this.criarAgendamentoUseCase = criarAgendamentoUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Agendamento criar(@RequestBody CriarAgendamentoCommand command) {
        return criarAgendamentoUseCase.execute(command);
    }
}
