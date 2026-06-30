package br.com.morbus.agendamento.domain.exception;

public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(String id) {
        super("Appointment com id '" + id + "' nao encontrado");
    }
}
