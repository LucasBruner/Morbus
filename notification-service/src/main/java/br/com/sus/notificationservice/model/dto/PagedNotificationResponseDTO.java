package br.com.sus.notificationservice.model.dto;

import java.util.List;

public record PagedNotificationResponseDTO(
        List<NotificationQueueDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
