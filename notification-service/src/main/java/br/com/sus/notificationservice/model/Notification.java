package br.com.sus.notificationservice.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntity {

    public String eventType;
    public String recipientName;
    public String recipientContact;
    public String message;
    public LocalDateTime sentAt;
    public String status;
}
