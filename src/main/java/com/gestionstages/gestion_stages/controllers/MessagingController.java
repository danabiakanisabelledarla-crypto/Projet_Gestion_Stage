package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Conversation;
import com.gestionstages.gestion_stages.entities.Message;
import com.gestionstages.gestion_stages.entities.Utilisateur;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import com.gestionstages.gestion_stages.services.MessagingService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/messagerie")
public class MessagingController {

    private static final Path DOSSIER_MESSAGES = Paths.get("uploads", "messages")
            .toAbsolutePath().normalize();

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostMapping("/nouveau")
    public String nouvelleConversation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestParam Integer destinataireId,
                                       @RequestParam String message,
                                       RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = userDetails.getUtilisateur();
        String baseUrl = messagingService.baseUrl(utilisateur);
        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("erreurMessage", "Le message ne peut pas etre vide.");
            return "redirect:" + baseUrl;
        }
        try {
            Conversation conversation = messagingService
                    .obtenirOuCreerConversation(utilisateur, destinataireId);
            messagingService.enregistrerTexte(conversation, utilisateur, message);
            return "redirect:" + baseUrl + "?convId=" + conversation.getId();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("erreurMessage", exception.getMessage());
            return "redirect:" + baseUrl;
        }
    }

    @PostMapping(value = "/envoyer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String envoyer(@AuthenticationPrincipal CustomUserDetails userDetails,
                          @RequestParam Integer convId,
                          @RequestParam(required = false) String contenu,
                          @RequestParam(required = false) List<MultipartFile> fichiers,
                          RedirectAttributes redirectAttributes) {
        Utilisateur utilisateur = userDetails.getUtilisateur();
        String baseUrl = messagingService.baseUrl(utilisateur);
        Conversation conversation = messagingService
                .conversationAccessible(convId, utilisateur.getId())
                .orElse(null);
        if (conversation == null) {
            redirectAttributes.addFlashAttribute("erreurMessage", "Conversation inaccessible.");
            return "redirect:" + baseUrl;
        }
        boolean textePresent = contenu != null && !contenu.isBlank();
        boolean fichiersPresents = fichiers != null && fichiers.stream().anyMatch(fichier -> !fichier.isEmpty());
        if (!textePresent && !fichiersPresents) {
            redirectAttributes.addFlashAttribute("erreurMessage", "Ajoutez un message ou un fichier.");
            return "redirect:" + baseUrl + "?convId=" + convId;
        }
        if (textePresent) {
            messagingService.enregistrerTexte(conversation, utilisateur, contenu);
        }
        if (fichiersPresents) {
            try {
                Path dossierConversation = DOSSIER_MESSAGES.resolve(String.valueOf(convId)).normalize();
                Files.createDirectories(dossierConversation);
                for (MultipartFile fichier : fichiers) {
                    if (fichier.isEmpty()) continue;
                    String original = fichier.getOriginalFilename() == null
                            ? "fichier"
                            : Paths.get(fichier.getOriginalFilename()).getFileName().toString();
                    String nomStockage = UUID.randomUUID() + "_" + nettoyerNom(original);
                    Path destination = dossierConversation.resolve(nomStockage).normalize();
                    if (!destination.startsWith(dossierConversation)) {
                        throw new IOException("Nom de fichier invalide");
                    }
                    Files.copy(fichier.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
                    messagingService.enregistrerPieceJointe(
                            conversation,
                            utilisateur,
                            original,
                            destination.toString(),
                            fichier.getContentType() == null
                                    ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                    : fichier.getContentType(),
                            fichier.getSize());
                }
            } catch (IOException exception) {
                redirectAttributes.addFlashAttribute("erreurMessage",
                        "Un fichier n'a pas pu etre enregistre.");
            }
        }
        return "redirect:" + baseUrl + "?convId=" + convId;
    }

    @GetMapping("/fichiers/{messageId}")
    public ResponseEntity<Resource> telecharger(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                @PathVariable Integer messageId) {
        return servirFichier(userDetails, messageId, false);
    }

    @GetMapping("/fichiers/{messageId}/apercu")
    public ResponseEntity<Resource> apercu(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @PathVariable Integer messageId) {
        return servirFichier(userDetails, messageId, true);
    }

    @GetMapping("/actualiser")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualiser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer convId,
            @RequestParam(defaultValue = "0") Integer after) {
        try {
            return ResponseEntity.ok(messagingService.actualiser(
                    userDetails.getUtilisateur(), convId, after));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    private ResponseEntity<Resource> servirFichier(CustomUserDetails userDetails,
                                                   Integer messageId,
                                                   boolean inline) {
        Message message = messagingService.messageAccessible(
                        messageId, userDetails.getUtilisateur().getId())
                .filter(item -> item.getCheminFichier() != null)
                .orElse(null);
        if (message == null) return ResponseEntity.notFound().build();
        Path chemin = Paths.get(message.getCheminFichier()).toAbsolutePath().normalize();
        if (!chemin.startsWith(DOSSIER_MESSAGES) || !Files.isRegularFile(chemin)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(message.getTypeMime());
        } catch (Exception exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(message.getNomFichier(), java.nio.charset.StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(message.getTailleFichier() == null ? 0 : message.getTailleFichier())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(chemin));
    }

    private String nettoyerNom(String nom) {
        String nettoye = nom.replaceAll("[^a-zA-Z0-9._-]", "_");
        return nettoye.isBlank() ? "fichier" : nettoye.toLowerCase(Locale.ROOT);
    }
}
