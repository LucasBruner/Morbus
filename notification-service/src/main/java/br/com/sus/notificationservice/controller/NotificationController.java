package br.com.sus.notificationservice.controller;

import br.com.sus.notificationservice.model.Notification;
import br.com.sus.notificationservice.model.dto.NotificationQueueDTO;
import br.com.sus.notificationservice.model.dto.PagedNotificationResponseDTO;
import br.com.sus.notificationservice.model.dto.ProblemDetailDTO;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/notifications")
@Tag(name = "Notifications", description = "Gerenciamento de notificações enviadas")
public class NotificationController {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Context
    UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar notificações", description = "Retorna notificações paginadas, com filtro opcional por tipo de evento")
    @APIResponse(responseCode = "200", description = "Página de notificações retornada com sucesso")
    public Response getNotifications(
            @Parameter(description = "Tipo do evento (ex: PATIENT_REGISTERED, PATIENT_CALLED, PRIORITY_UPDATED, PATIENT_CANCELLED)")
            @QueryParam("eventType") String eventType,
            @Parameter(description = "Número da página (zero-based)")
            @QueryParam("page") @jakarta.ws.rs.DefaultValue("0") int page,
            @Parameter(description = "Itens por página")
            @QueryParam("size") @jakarta.ws.rs.DefaultValue("20") int size) {

        Sort sort = Sort.by("sentAt").descending();
        var query = eventType != null
                ? Notification.find("eventType", sort, eventType)
                : Notification.findAll(sort);
        query = query.page(Page.of(page, size));

        List<NotificationQueueDTO> content = query.<Notification>list().stream()
                .map(this::toNotificationQueueDTO)
                .toList();
        long totalElements = query.count();
        int totalPages = query.pageCount();

        return Response.ok(new PagedNotificationResponseDTO(content, page, size, totalElements, totalPages)).build();
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
        Notification notification = Notification.findById(id);

        if (notification == null) {
            ProblemDetailDTO problem = ProblemDetailDTO.of(
                    "notification-not-found",
                    "Recurso não encontrado",
                    "Notificação com id '" + id + "' não encontrada",
                    404,
                    uriInfo.getPath());
            return Response.status(Response.Status.NOT_FOUND)
                    .type(PROBLEM_JSON)
                    .entity(problem)
                    .build();
        }

        return Response.ok(toNotificationQueueDTO(notification)).build();
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
