package br.com.sus.notification.repository;

import br.com.sus.notification.model.Notification;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public interface NotificationRepository extends PanacheRepository<Notification> {
}
