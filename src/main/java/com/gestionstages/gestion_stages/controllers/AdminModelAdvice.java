package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Notification;
import com.gestionstages.gestion_stages.repositories.NotificationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class AdminModelAdvice {

    private final NotificationRepository notificationRepository;

    public AdminModelAdvice(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @ModelAttribute("notificationsCount")
    public long getNotificationsCount(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/admin/")) {
            return notificationRepository.count();
        }
        return 0;
    }

    @ModelAttribute("recentNotifications")
    public List<Notification> getRecentNotifications(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/admin/")) {
            return notificationRepository.findAllByOrderByDateEnvoiDesc()
                    .stream().limit(5).toList();
        }
        return List.of();
    }
}