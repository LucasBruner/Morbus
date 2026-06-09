package br.com.sus.notificationservice.controller;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.dto.NotificationQueueDTO;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

@Path("/api/v1/notifications")
@Tag(name = "Notifications", description = "Gerenciamento de notificações enviadas")
public class NotificationController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar notificações", description = "Retorna todas as notificações ou filtra por tipo de evento")
    @APIResponse(responseCode = "200", description = "Lista de notificações retornada com sucesso")
    public Response getNotifications(
            @Parameter(description = "Tipo do evento (ex: PATIENT_REGISTERED, PATIENT_CALLED, PRIORITY_UPDATED, PATIENT_CANCELLED)")
            @QueryParam("eventType") String eventType) {
        List<Notification> notifications = eventType != null
                ? Notification.list("eventType", Sort.by("sentAt").descending(), eventType)
                : Notification.listAll(Sort.by("sentAt").descending());
        List<NotificationQueueDTO> dtos = notifications.stream()
                .map(this::toNotificationQueueDTO)
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Buscar notificação por ID", description = "Retorna uma notificação pelo seu identificador")
    @APIResponse(responseCode = "200", description = "Notificação encontrada")
    @APIResponse(responseCode = "404", description = "Notificação não encontrada")
    public Response findNotificationById(
            @Parameter(description = "ID da notificação", required = true)
            @PathParam("id") Long id) {
        try {
            Notification notification = Notification.findById(id);
            if (notification == null) return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(toNotificationQueueDTO(notification)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Notificação não encontrada para o id: " + id))
                    .build();
        }
    }

    private NotificationQueueDTO toNotificationQueueDTO(Notification notification) {
        return new NotificationQueueDTO(notification.id,
                notification.eventType,
                notification.recipientName,
                notification.recipientContact,
                notification.message,
                notification.sentAt,
                notification.status);
    }
}
