package br.com.sus.notification.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "notification_id_seq")
    private Long id;
    private String eventType;
    private String recipientName;
    private String recipientContact;
    private String message;
    private LocalDateTime sentAt;
    private String status;
}
