package br.com.morbus.agendamento.infrastructure.config;

import br.com.morbus.agendamento.domain.CriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ICriarAgendamentoUseCase criarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository) {
        return new CriarAgendamentoUseCase(agendamentoRepository);
    }
}
