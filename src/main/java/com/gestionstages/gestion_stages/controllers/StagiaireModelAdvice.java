package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Notification;
import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.repositories.ConversationRepository;
import com.gestionstages.gestion_stages.repositories.NotificationRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(assignableTypes = StagiaireController.class)
public class StagiaireModelAdvice {

    private final NotificationRepository notificationRepository;
    private final ConversationRepository conversationRepository;

    public StagiaireModelAdvice(NotificationRepository notificationRepository,
                                ConversationRepository conversationRepository) {
        this.notificationRepository = notificationRepository;
        this.conversationRepository = conversationRepository;
    }

    @ModelAttribute("notificationsStagiaire")
    public List<Notification> notifications(Authentication authentication) {
        Utilisateur utilisateur = authenticatedUser(authentication);
        return utilisateur == null
                ? List.of()
                : notificationRepository.findTop8ByDestinataireEmailOrderByDateEnvoiDesc(
                        utilisateur.getEmail());
    }

    @ModelAttribute("notificationsCount")
    public long notificationsCount(Authentication authentication) {
        Utilisateur utilisateur = authenticatedUser(authentication);
        return utilisateur == null ? 0
                : notificationRepository.countByDestinataireEmailAndStatutNot(
                        utilisateur.getEmail(), "lue");
    }

    @ModelAttribute("messagesCount")
    public long messagesCount(Authentication authentication) {
        Utilisateur utilisateur = authenticatedUser(authentication);
        if (utilisateur == null) {
            return 0;
        }
        return conversationRepository
                .findByParticipantIdOrderByDernierMessageDesc(utilisateur.getId())
                .stream()
                .mapToLong(conversation -> conversationRepository
                        .countNonLuByConversation(conversation.getId(), utilisateur.getId()))
                .sum();
    }

    @ModelAttribute("nomComplet")
    public String nomComplet(Authentication authentication) {
        Utilisateur utilisateur = authenticatedUser(authentication);
        return utilisateur == null ? "" : utilisateur.getPrenom() + " " + utilisateur.getNom();
    }

    @ModelAttribute("initiales")
    public String initiales(Authentication authentication) {
        Utilisateur utilisateur = authenticatedUser(authentication);
        return utilisateur == null ? "" : initiale(utilisateur.getPrenom()) + initiale(utilisateur.getNom());
    }

    private Utilisateur authenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return details.getUtilisateur();
    }

    private String initiale(String valeur) {
        return valeur == null || valeur.isBlank() ? "" : valeur.substring(0, 1).toUpperCase();
    }
}
