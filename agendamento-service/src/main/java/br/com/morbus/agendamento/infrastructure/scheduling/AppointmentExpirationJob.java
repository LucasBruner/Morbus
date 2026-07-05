package br.com.morbus.agendamento.infrastructure.scheduling;

import br.com.morbus.agendamento.application.usecase.ExpirarAgendamentosUseCase;
import br.com.morbus.agendamento.domain.model.Agendamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AppointmentExpirationJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentExpirationJob.class);
    private final ExpirarAgendamentosUseCase expirarAgendamentosUseCase;

    public AppointmentExpirationJob(ExpirarAgendamentosUseCase expirarAgendamentosUseCase) {
        this.expirarAgendamentosUseCase = expirarAgendamentosUseCase;
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void run() {
        List<Agendamento> expirados = expirarAgendamentosUseCase.findExpiredAppointments();
        AtomicInteger expiredCount = new AtomicInteger();

        expirados.forEach(agendamento -> {
            try {
                expirarAgendamentosUseCase.expireAppointment(agendamento);
                expiredCount.incrementAndGet();
            } catch (Exception exception) {
                LOGGER.error("Falha ao expirar agendamento {}: {}", agendamento.getId(), exception.getMessage(), exception);
            }
        });

        LOGGER.info("Job de expiracao executado. Agendamentos expirados com sucesso: {}. Total encontrados: {}.",
                expiredCount.get(), expirados.size());
    }
}
