package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findAllByOrderByDateEnvoiDesc();
    
    long countByDestinataireTypeAndStatut(String destinataireType, String statut);
    
    long countByDestinataireType(String destinataireType);
}