package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findAllByOrderByDateEnvoiDesc();

    List<Notification> findTop8ByDestinataireEmailOrderByDateEnvoiDesc(String destinataireEmail);

    long countByDestinataireEmailAndStatut(String destinataireEmail, String statut);

    long countByDestinataireEmailAndStatutNot(String destinataireEmail, String statut);

    List<Notification> findByDestinataireEmailAndStatutNot(String destinataireEmail, String statut);
    
    long countByDestinataireTypeAndStatut(String destinataireType, String statut);
    
    long countByDestinataireType(String destinataireType);

    List<Notification> findTop8ByDestinataireTypeOrderByDateEnvoiDesc(String destinataireType);

    long countByDestinataireTypeAndStatutNot(String destinataireType, String statut);

    List<Notification> findByDestinataireTypeAndStatutNot(String destinataireType, String statut);
}
