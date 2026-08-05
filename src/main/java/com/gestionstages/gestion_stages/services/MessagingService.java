package com.gestionstages.gestion_stages.services;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final EncadreurRepository encadreurRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final PresenceService presenceService;

    public MessagingService(ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            UtilisateurRepository utilisateurRepository,
                            EncadreurRepository encadreurRepository,
                            StagiaireRepository stagiaireRepository,
                            StageRepository stageRepository,
                            PresenceService presenceService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.encadreurRepository = encadreurRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.stageRepository = stageRepository;
        this.presenceService = presenceService;
    }

    public void preparerModele(Model model,
                               Utilisateur utilisateur,
                               Integer conversationId,
                               String baseUrl,
                               String espace) {
        presenceService.markOnline(utilisateur.getId());
        List<Conversation> conversations = conversationRepository
                .findByParticipantIdOrderByDernierMessageDesc(utilisateur.getId());
        Conversation active = conversationId == null
                ? conversations.stream().findFirst().orElse(null)
                : conversationAccessible(conversationId, utilisateur.getId()).orElse(null);
        List<Message> messages = active == null
                ? new ArrayList<>()
                : messageRepository.findByConversationIdOrderByDateEnvoiAsc(active.getId());
        boolean messagesModifies = false;
        for (Message message : messages) {
            if (!message.getExpediteur().getId().equals(utilisateur.getId())
                    && !Boolean.TRUE.equals(message.getLu())) {
                message.setLu(true);
                messagesModifies = true;
            }
        }
        if (messagesModifies) {
            messageRepository.saveAll(messages);
        }

        Map<Integer, Long> nonLus = new HashMap<>();
        Map<Integer, Message> derniersMessages = new HashMap<>();
        Map<Integer, Boolean> presences = new HashMap<>();
        conversations.forEach(conversation -> nonLus.put(conversation.getId(),
                conversationRepository.countNonLuByConversation(conversation.getId(), utilisateur.getId())));
        conversations.forEach(conversation -> {
            messageRepository.findFirstByConversationIdOrderByDateEnvoiDesc(conversation.getId())
                    .ifPresent(message -> derniersMessages.put(conversation.getId(), message));
            autreParticipant(conversation, utilisateur.getId())
                    .ifPresent(contact -> presences.put(contact.getId(), presenceService.isOnline(contact.getId())));
        });

        model.addAttribute("user", utilisateur);
        model.addAttribute("conversations", conversations);
        model.addAttribute("activeConversation", active);
        model.addAttribute("messages", messages);
        model.addAttribute("contacts", contactsDisponibles(utilisateur));
        model.addAttribute("nonLusParConversation", nonLus);
        model.addAttribute("dernierMessageParConversation", derniersMessages);
        model.addAttribute("presenceParUtilisateur", presences);
        model.addAttribute("messageBase", baseUrl);
        model.addAttribute("workspaceTitle", espace);
    }

    public Map<String, Object> actualiser(Utilisateur utilisateur,
                                          Integer conversationId,
                                          Integer dernierMessageId) {
        presenceService.markOnline(utilisateur.getId());
        Conversation conversation = conversationAccessible(conversationId, utilisateur.getId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation inaccessible."));
        List<Message> nouveauxMessages = messageRepository
                .findByConversationIdAndIdGreaterThanOrderByDateEnvoiAsc(
                        conversationId, dernierMessageId == null ? 0 : dernierMessageId);

        boolean messagesModifies = false;
        for (Message message : nouveauxMessages) {
            if (!message.getExpediteur().getId().equals(utilisateur.getId())
                    && !Boolean.TRUE.equals(message.getLu())) {
                message.setLu(true);
                messagesModifies = true;
            }
        }
        if (messagesModifies) {
            messageRepository.saveAll(nouveauxMessages);
        }

        List<Map<String, Object>> messages = nouveauxMessages.stream()
                .map(message -> messageJson(message, utilisateur.getId()))
                .toList();
        List<Map<String, Object>> conversations = conversationRepository
                .findByParticipantIdOrderByDernierMessageDesc(utilisateur.getId())
                .stream()
                .map(item -> conversationJson(item, utilisateur))
                .toList();
        Utilisateur autre = autreParticipant(conversation, utilisateur.getId()).orElse(utilisateur);

        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("messages", messages);
        resultat.put("messagesLus", messageRepository
                .findByConversationIdOrderByDateEnvoiAsc(conversationId)
                .stream()
                .filter(message -> message.getExpediteur().getId().equals(utilisateur.getId()))
                .filter(message -> Boolean.TRUE.equals(message.getLu()))
                .map(Message::getId)
                .toList());
        resultat.put("conversations", conversations);
        resultat.put("contactEnLigne", presenceService.isOnline(autre.getId()));
        resultat.put("dernierMessageId", messageRepository
                .findFirstByConversationIdOrderByDateEnvoiDesc(conversationId)
                .map(Message::getId)
                .orElse(0));
        return resultat;
    }

    public Optional<Conversation> conversationAccessible(Integer conversationId, Integer utilisateurId) {
        return conversationRepository.findById(conversationId)
                .filter(conversation -> conversation.getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(utilisateurId)));
    }

    public Optional<Message> messageAccessible(Integer messageId, Integer utilisateurId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getConversation().getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(utilisateurId)));
    }

    public Conversation obtenirOuCreerConversation(Utilisateur expediteur, Integer destinataireId) {
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .filter(utilisateur -> peutContacter(expediteur, utilisateur))
                .orElseThrow(() -> new IllegalArgumentException("Destinataire non autorise"));
        return conversationRepository.findByParticipantIdOrderByDernierMessageDesc(expediteur.getId())
                .stream()
                .filter(conversation -> conversation.getParticipants().size() == 2
                        && conversation.getParticipants().stream()
                        .anyMatch(participant -> participant.getId().equals(destinataireId)))
                .findFirst()
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setSujet("Discussion avec " + destinataire.getPrenom() + " " + destinataire.getNom());
                    conversation.setDateCreation(LocalDateTime.now());
                    conversation.getParticipants().add(expediteur);
                    conversation.getParticipants().add(destinataire);
                    return conversationRepository.save(conversation);
                });
    }

    public Message enregistrerTexte(Conversation conversation, Utilisateur expediteur, String contenu) {
        Message message = creerMessage(conversation, expediteur);
        message.setContenu(contenu == null ? "" : contenu.trim());
        mettreAJourConversation(conversation);
        return messageRepository.save(message);
    }

    public Message enregistrerPieceJointe(Conversation conversation,
                                          Utilisateur expediteur,
                                          String nomFichier,
                                          String cheminFichier,
                                          String typeMime,
                                          long taille) {
        Message message = creerMessage(conversation, expediteur);
        message.setContenu("");
        message.setNomFichier(nomFichier);
        message.setCheminFichier(cheminFichier);
        message.setTypeMime(typeMime);
        message.setTypePieceJointe(typeMime != null && typeMime.startsWith("image/") ? "image" : "fichier");
        message.setTailleFichier(taille);
        mettreAJourConversation(conversation);
        return messageRepository.save(message);
    }

    public String baseUrl(Utilisateur utilisateur) {
        return switch (espace(utilisateur)) {
            case "ADMINISTRATEUR" -> "/admin/messages";
            case "RESPONSABLE_STAGE" -> "/responsable/messages";
            case "ENCADREUR" -> "/encadreur/messagerie";
            case "STAGIAIRE" -> "/stagiaire/messages";
            default -> "/redirection";
        };
    }

    private Message creerMessage(Conversation conversation, Utilisateur expediteur) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setExpediteur(expediteur);
        message.setDateEnvoi(LocalDateTime.now());
        message.setLu(false);
        return message;
    }

    private void mettreAJourConversation(Conversation conversation) {
        conversation.setDernierMessage(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    private List<Utilisateur> contactsDisponibles(Utilisateur utilisateur) {
        LinkedHashMap<Integer, Utilisateur> contacts = new LinkedHashMap<>();
        String espace = espace(utilisateur);
        if ("ADMINISTRATEUR".equals(espace)) {
            utilisateurRepository.findAll().forEach(contact -> ajouterContact(contacts, utilisateur, contact));
        } else if ("RESPONSABLE_STAGE".equals(espace)) {
            ajouterRoles(contacts, utilisateur, "ADMINISTRATEUR", "ENCADREUR", "STAGIAIRE");
        } else if ("ENCADREUR".equals(espace)) {
            ajouterRoles(contacts, utilisateur, "ADMINISTRATEUR", "RESPONSABLE_STAGE");
            encadreurRepository.findByUtilisateurId(utilisateur.getId())
                    .map(encadreur -> stageRepository.findByEncadreurId(encadreur.getId()))
                    .orElseGet(ArrayList::new)
                    .stream()
                    .map(Stage::getStagiaire)
                    .filter(Objects::nonNull)
                    .map(Stagiaire::getUtilisateur)
                    .forEach(contact -> ajouterContact(contacts, utilisateur, contact));
        } else if ("STAGIAIRE".equals(espace)) {
            ajouterRoles(contacts, utilisateur, "ADMINISTRATEUR", "RESPONSABLE_STAGE");
            stagiaireRepository.findByUtilisateurId(utilisateur.getId())
                    .flatMap(stagiaire -> stageRepository.findByStagiaireId(stagiaire.getId()))
                    .map(Stage::getEncadreur)
                    .map(Encadreur::getUtilisateur)
                    .ifPresent(contact -> ajouterContact(contacts, utilisateur, contact));
        }
        return contacts.values().stream()
                .sorted(Comparator.comparing(Utilisateur::getPrenom).thenComparing(Utilisateur::getNom))
                .toList();
    }

    private void ajouterRoles(Map<Integer, Utilisateur> contacts,
                              Utilisateur utilisateur,
                              String... roles) {
        for (String role : roles) {
            utilisateurRepository.findByRole_Libelle(role)
                    .forEach(contact -> ajouterContact(contacts, utilisateur, contact));
        }
    }

    private void ajouterContact(Map<Integer, Utilisateur> contacts,
                                Utilisateur utilisateur,
                                Utilisateur contact) {
        if (!contact.getId().equals(utilisateur.getId())
                && contact.getStatut() == Utilisateur.StatutUtilisateur.actif) {
            contacts.put(contact.getId(), contact);
        }
    }

    private boolean peutContacter(Utilisateur expediteur, Utilisateur destinataire) {
        return contactsDisponibles(expediteur).stream()
                .anyMatch(contact -> contact.getId().equals(destinataire.getId()));
    }

    private String espace(Utilisateur utilisateur) {
        return utilisateur.getRole() == null ? "" : utilisateur.getRole().getEspaceEffectif();
    }

    private Optional<Utilisateur> autreParticipant(Conversation conversation, Integer utilisateurId) {
        return conversation.getParticipants().stream()
                .filter(participant -> !participant.getId().equals(utilisateurId))
                .findFirst();
    }

    private Map<String, Object> conversationJson(Conversation conversation, Utilisateur utilisateur) {
        Utilisateur autre = autreParticipant(conversation, utilisateur.getId()).orElse(utilisateur);
        Message dernier = messageRepository
                .findFirstByConversationIdOrderByDateEnvoiDesc(conversation.getId())
                .orElse(null);
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("id", conversation.getId());
        resultat.put("nom", autre.getPrenom() + " " + autre.getNom());
        resultat.put("email", autre.getEmail());
        resultat.put("role", autre.getRole().getEspaceEffectif());
        resultat.put("enLigne", presenceService.isOnline(autre.getId()));
        resultat.put("nonLus", conversationRepository
                .countNonLuByConversation(conversation.getId(), utilisateur.getId()));
        resultat.put("dernierMessage", dernier == null ? "" : resumeMessage(dernier));
        resultat.put("date", dernier == null ? conversation.getDateCreation() : dernier.getDateEnvoi());
        return resultat;
    }

    private Map<String, Object> messageJson(Message message, Integer utilisateurId) {
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("id", message.getId());
        resultat.put("contenu", message.getContenu());
        resultat.put("envoye", message.getExpediteur().getId().equals(utilisateurId));
        resultat.put("lu", Boolean.TRUE.equals(message.getLu()));
        resultat.put("date", message.getDateEnvoi());
        resultat.put("expediteur", message.getExpediteur().getPrenom() + " "
                + message.getExpediteur().getNom());
        resultat.put("initiales", message.getExpediteur().getPrenom().substring(0, 1)
                + message.getExpediteur().getNom().substring(0, 1));
        resultat.put("typePieceJointe", message.getTypePieceJointe());
        resultat.put("nomFichier", message.getNomFichier());
        resultat.put("tailleFichier", message.getTailleFichier());
        resultat.put("apercuUrl", message.getTypePieceJointe() == null ? null
                : "/messagerie/fichiers/" + message.getId() + "/apercu");
        resultat.put("telechargementUrl", message.getTypePieceJointe() == null ? null
                : "/messagerie/fichiers/" + message.getId());
        return resultat;
    }

    private String resumeMessage(Message message) {
        if (message.getContenu() != null && !message.getContenu().isBlank()) {
            String contenu = message.getContenu().replaceAll("\\s+", " ").trim();
            return contenu.length() > 55 ? contenu.substring(0, 52) + "..." : contenu;
        }
        return "image".equals(message.getTypePieceJointe())
                ? "Image"
                : (message.getNomFichier() == null ? "Pièce jointe" : message.getNomFichier());
    }
}
