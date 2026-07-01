package br.com.morbus.agendamento.infrastructure.config;

import br.com.morbus.agendamento.application.usecase.BlockSlotUseCase;
import br.com.morbus.agendamento.application.usecase.CriarAgendamentoUseCase;
import br.com.morbus.agendamento.application.usecase.CriarScheduleUseCase;
import br.com.morbus.agendamento.application.usecase.UnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IScheduleRepository;
import br.com.morbus.agendamento.domain.port.out.ISlotRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ICriarAgendamentoUseCase criarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository) {
        return new CriarAgendamentoUseCase(agendamentoRepository);
    }

    @Bean
    public ICriarScheduleUseCase criarScheduleUseCase(IScheduleRepository scheduleRepository,
                                                      ISlotRepository slotRepository) {
        return new CriarScheduleUseCase(scheduleRepository, slotRepository);
    }

    @Bean
    public IBlockSlotUseCase blockSlotUseCase(ISlotRepository slotRepository) {
        return new BlockSlotUseCase(slotRepository);
    }

    @Bean
    public IUnblockSlotUseCase unblockSlotUseCase(ISlotRepository slotRepository) {
        return new UnblockSlotUseCase(slotRepository);
    }
}
