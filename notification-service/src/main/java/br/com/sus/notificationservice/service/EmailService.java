package br.com.sus.notificationservice.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailService {

    public void send(String destinatario, String assunto, String corpo) {
        System.out.printf("[EMAIL SIMULADO] Para: %s | Assunto: %s | Corpo: %s%n", destinatario, assunto, corpo);
    }
}
