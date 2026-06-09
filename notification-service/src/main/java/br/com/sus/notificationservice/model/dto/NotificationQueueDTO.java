package br.com.sus.notificationservice.model.dto;

import br.com.sus.notificationservice.model.enums.ENotificationType;

import java.time.LocalDateTime;

public record NotificationQueueDTO (Long id,
                                    String eventType,
                                    String recipientName,
                                    String recipientContact,
                                    String message,
                                    LocalDateTime sentAt,
                                    String status){
}
