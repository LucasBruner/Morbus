package br.com.morbus.agendamento.infrastructure.config;

import br.com.morbus.agendamento.adapter.out.rabbitmq.IAgendamentoEventPublisher;
import br.com.morbus.agendamento.application.usecase.*;
import br.com.morbus.agendamento.domain.port.in.IAgendamentosPacienteUseCase;
import br.com.morbus.agendamento.domain.port.in.IAlocarPacienteEmSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IAtenderAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IBlockSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IRegistrarFaltaAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICancelarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IConsultarDisponibilidadeUseCase;
import br.com.morbus.agendamento.domain.port.in.IConfirmarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.ICriarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.in.IConsultarGradeUseCase;
import br.com.morbus.agendamento.domain.port.in.IDetalharAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IReagendarAgendamentoUseCase;
import br.com.morbus.agendamento.domain.port.in.IAtualizarScheduleUseCase;
import br.com.morbus.agendamento.domain.port.in.IUnblockSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IAgendamentoRepository;
import br.com.morbus.agendamento.domain.port.out.IHealthUnitRepository;
import br.com.morbus.agendamento.domain.port.out.IProviderRepository;
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
                                                                    ISlotRepository slotRepository,
                                                                    IScheduleRepository scheduleRepository,
                                                                    IHealthUnitRepository healthUnitRepository) {
        return new ConfirmarAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository, healthUnitRepository);
    }

    @Bean
    public IAtenderAgendamentoUseCase atenderAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                ISlotRepository slotRepository,
                                                                IScheduleRepository scheduleRepository,
                                                                IAgendamentoEventPublisher eventPublisher) {
        return new AtenderAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository, eventPublisher);
    }

    @Bean
    public IRegistrarFaltaAgendamentoUseCase registrarFaltaAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                              ISlotRepository slotRepository,
                                                                              IScheduleRepository scheduleRepository,
                                                                              IAgendamentoEventPublisher eventPublisher) {
        return new RegistrarFaltaAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository, eventPublisher);
    }

    @Bean
    public ExpirarAgendamentosUseCase expirarAgendamentosUseCase(IAgendamentoRepository agendamentoRepository,
                                                                 ISlotRepository slotRepository,
                                                                 IScheduleRepository scheduleRepository,
                                                                 IAgendamentoEventPublisher eventPublisher) {
        return new ExpirarAgendamentosUseCase(agendamentoRepository, slotRepository, scheduleRepository, eventPublisher);
    }

    @Bean
    public ICancelarAgendamentoUseCase cancelarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                  ISlotRepository slotRepository) {
        return new CancelarAgendamentoUseCase(agendamentoRepository, slotRepository);
    }

    @Bean
    public IReagendarAgendamentoUseCase reagendarAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                    ISlotRepository slotRepository,
                                                                    IAgendamentoEventPublisher eventPublisher) {
        return new ReagendarAgendamentoUseCase(agendamentoRepository, slotRepository, eventPublisher);
    }

    @Bean
    public IAtualizarScheduleUseCase atualizarScheduleUseCase(IScheduleRepository scheduleRepository) {
        return new AtualizarScheduleUseCase(scheduleRepository);
    }

    @Bean
    public IConsultarDisponibilidadeUseCase consultarDisponibilidadeUseCase(ISlotRepository slotRepository) {
        return new ConsultarDisponibilidadeUseCase(slotRepository);
    }

    @Bean
    public IAgendamentosPacienteUseCase agendamentosPacienteUseCase(IAgendamentoRepository agendamentoRepository) {
        return new AgendamentosPacienteUseCase(agendamentoRepository);
    }

    @Bean
    public IDetalharAgendamentoUseCase detalharAgendamentoUseCase(IAgendamentoRepository agendamentoRepository,
                                                                  ISlotRepository slotRepository,
                                                                  IScheduleRepository scheduleRepository,
                                                                  IHealthUnitRepository healthUnitRepository,
                                                                  IProviderRepository providerRepository) {
        return new DetalharAgendamentoUseCase(agendamentoRepository, slotRepository, scheduleRepository,
                healthUnitRepository, providerRepository);
    }

    @Bean
    public IConsultarGradeUseCase consultarGradeUseCase(IScheduleRepository scheduleRepository,
                                                        IHealthUnitRepository healthUnitRepository,
                                                        IProviderRepository providerRepository) {
        return new ConsultarGradeUseCase(scheduleRepository, healthUnitRepository, providerRepository);
    }
}
