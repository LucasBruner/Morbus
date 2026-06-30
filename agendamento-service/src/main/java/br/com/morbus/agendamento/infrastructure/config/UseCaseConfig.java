package br.com.morbus.agendamento.infrastructure.config;

import br.com.morbus.agendamento.application.usecase.AlocarSlotUseCase;
import br.com.morbus.agendamento.domain.port.in.IAlocarSlotUseCase;
import br.com.morbus.agendamento.domain.port.out.IAppointmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public IAlocarSlotUseCase alocarSlotUseCase(IAppointmentRepository appointmentRepository) {
        return new AlocarSlotUseCase(appointmentRepository);
    }
}
