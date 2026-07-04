package br.com.morbus.agendamento.infrastructure.config;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.application.usecase.AlocarPacienteEmSlotUseCase;
import br.com.morbus.agendamento.application.usecase.BlockSlotUseCase;
import br.com.morbus.agendamento.application.usecase.CancelarAgendamentoUseCase;
import br.com.morbus.agendamento.application.usecase.ConfirmarAgendamentoUseCase;
import br.com.morbus.agendamento.application.usecase.CriarAgendamentoUseCase;
import br.com.morbus.agendamento.application.usecase.CriarScheduleUseCase;
import br.com.morbus.agendamento.application.usecase.UnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IAlocarPacienteEmSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
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
    public IAlocarPacienteEmSlotUseCase alocarPacienteEmSlotUseCase(IAgendamentoRepository agendamentoRepository,
                                                                    ISlotRepository slotRepository,
                                                                    IAgendamentoEventPublisher eventPublisher) {
        return new AlocarPacienteEmSlotUseCase(agendamentoRepository, slotRepository, eventPublisher);
    }

    @Bean
    public ICriarScheduleUseCase criarScheduleUseCase(IScheduleRepository scheduleRepository,
                                                      ISlotRepository slotRepository) {
        return new CriarScheduleUseCase(scheduleRepository, slotRepository);
    }

    @Bean
    public IBlockSlotUseCase blockSlotUseCase(ISlotRepository slotRepository,
                                              IScheduleRepository scheduleRepository) {
        return new BlockSlotUseCase(slotRepository, scheduleRepository);
    }

    @Bean
    public IUnblockSlotUseCase unblockSlotUseCase(ISlotRepository slotRepository,
                                                  IScheduleRepository scheduleRepository) {
        return new UnblockSlotUseCase(slotRepository, scheduleRepository);
    }

    @Bean
    public IConfirmarAgendamentoUseCase confirmarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                    ISlotRepository slotRepository) {
        return new ConfirmarAgendamentoUseCase(agendamentoRepository, slotRepository);
    }

    @Bean
    public ICancelarAgendamentoUseCase cancelarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                  ISlotRepository slotRepository) {
        return new CancelarAgendamentoUseCase(agendamentoRepository, slotRepository);
    }
}
