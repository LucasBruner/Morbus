package br.com.morbus.agendamento.adapter.out.messaging;

import br.com.morbus.agendamento.domain.model.Agendamento;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoEventPublisherAdapter {

    public void publishCreated(Agendamento agendamento) {
    }
}
