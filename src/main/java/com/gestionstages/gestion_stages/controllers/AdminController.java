package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.gestionstages.gestion_stages.security.CustomUserDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DemandeStageRepository demandeStageRepository;
    private final StageRepository stageRepository;
    private final TacheRepository tacheRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EncadreurRepository encadreurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ServiceEntrepriseRepository serviceRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public AdminController(DemandeStageRepository demandeStageRepository,
                            StageRepository stageRepository,
                            TacheRepository tacheRepository,
                            StagiaireRepository stagiaireRepository,
                            EncadreurRepository encadreurRepository,
                            UtilisateurRepository utilisateurRepository,
                            ServiceEntrepriseRepository serviceRepository,
                            DocumentRepository documentRepository,
                            NotificationRepository notificationRepository,
                            ActivityLogRepository activityLogRepository,
                            ConversationRepository conversationRepository,
                            MessageRepository messageRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.stageRepository = stageRepository;
        this.tacheRepository = tacheRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.encadreurRepository = encadreurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.serviceRepository = serviceRepository;
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // ============================================
    // MÉTHODE DASHBOARD CORRIGÉE
    // ============================================
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(value = "periode", defaultValue = "7") int periode,
            Model model) {

        // --- STATISTIQUES ---
        model.addAttribute("statUtilisateurs", 128);
        model.addAttribute("statStagiaires", 72);
        model.addAttribute("statEncadreurs", 18);
        model.addAttribute("statResponsables", 6);
        model.addAttribute("statDocuments", 256);
        model.addAttribute("statActivites", 342);
        model.addAttribute("totalConnexions", 126);

        // --- ACTIVITÉS RÉCENTES ---
        List<Map<String, String>> activitesRecentes = new ArrayList<>();
        
        Map<String, String> act1 = new HashMap<>();
        act1.put("icone", "fas fa-user-plus");
        act1.put("couleur", "bleu-clair");
        act1.put("message", "Nouvel utilisateur créé");
        act1.put("temps", "Il y a 2 min");
        activitesRecentes.add(act1);
        
        Map<String, String> act2 = new HashMap<>();
        act2.put("icone", "fas fa-file-pdf");
        act2.put("couleur", "vert-clair");
        act2.put("message", "Document téléchargé");
        act2.put("temps", "Il y a 15 min");
        activitesRecentes.add(act2);
        
        Map<String, String> act3 = new HashMap<>();
        act3.put("icone", "fas fa-times-circle");
        act3.put("couleur", "rouge-clair");
        act3.put("message", "Connexion échouée");
        act3.put("temps", "Il y a 1h");
        activitesRecentes.add(act3);
        
        Map<String, String> act4 = new HashMap<>();
        act4.put("icone", "fas fa-database");
        act4.put("couleur", "turquoise-clair");
        act4.put("message", "Sauvegarde automatique");
        act4.put("temps", "Il y a 2h");
        activitesRecentes.add(act4);
        
        Map<String, String> act5 = new HashMap<>();
        act5.put("icone", "fas fa-sliders-h");
        act5.put("couleur", "orange-clair");
        act5.put("message", "Paramètre modifié");
        act5.put("temps", "Il y a 3h");
        activitesRecentes.add(act5);
        
        model.addAttribute("activitesRecentes", activitesRecentes);

        // --- STATUT SYSTÈME ---
        List<Map<String, String>> statutSysteme = new ArrayList<>();
        
        Map<String, String> stat1 = new HashMap<>();
        stat1.put("icone", "fas fa-server");
        stat1.put("nom", "Serveur");
        stat1.put("statut", "Opérationnel");
        stat1.put("couleur", "verte");
        statutSysteme.add(stat1);
        
        Map<String, String> stat2 = new HashMap<>();
        stat2.put("icone", "fas fa-database");
        stat2.put("nom", "Base de données");
        stat2.put("statut", "À jour");
        stat2.put("couleur", "verte");
        statutSysteme.add(stat2);
        
        Map<String, String> stat3 = new HashMap<>();
        stat3.put("icone", "fas fa-hdd");
        stat3.put("nom", "Stockage");
        stat3.put("statut", "78% utilisé");
        stat3.put("couleur", "orange");
        statutSysteme.add(stat3);
        
        Map<String, String> stat4 = new HashMap<>();
        stat4.put("icone", "fas fa-cloud-upload-alt");
        stat4.put("nom", "Sauvegarde");
        stat4.put("statut", "Aucun incident");
        stat4.put("couleur", "verte");
        statutSysteme.add(stat4);
        
        Map<String, String> stat5 = new HashMap<>();
        stat5.put("icone", "fas fa-shield-alt");
        stat5.put("nom", "Sécurité");
        stat5.put("statut", "Protégé");
        stat5.put("couleur", "verte");
        statutSysteme.add(stat5);
        
        model.addAttribute("statutSysteme", statutSysteme);

        // --- UTILISATEURS RÉCENTS ---
        List<Map<String, String>> utilisateursRecents = new ArrayList<>();
        
        Map<String, String> user1 = new HashMap<>();
        user1.put("avatar", "user1.jpg");
        user1.put("nom", "Marie Dupont");
        user1.put("role", "encadreur");
        user1.put("temps", "2h");
        utilisateursRecents.add(user1);
        
        Map<String, String> user2 = new HashMap<>();
        user2.put("avatar", "user2.jpg");
        user2.put("nom", "Jean Martin");
        user2.put("role", "stagiaire");
        user2.put("temps", "4h");
        utilisateursRecents.add(user2);
        
        Map<String, String> user3 = new HashMap<>();
        user3.put("avatar", "user3.jpg");
        user3.put("nom", "Sophie Legrand");
        user3.put("role", "responsable");
        user3.put("temps", "6h");
        utilisateursRecents.add(user3);
        
        model.addAttribute("utilisateursRecents", utilisateursRecents);

        return "admin/dashboard";
    }

    // ============================================
    // VOS AUTRES MÉTHODES (inchangées)
    // ============================================
    
    @GetMapping("/demandes")
    public String afficherDemandes(Model model) {
        model.addAttribute("activePage", "demandes");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "A");

        List<DemandeStage> toutesLesDemandes = demandeStageRepository.findAll();

        long totalDemandes = toutesLesDemandes.size();
        long enAttente = toutesLesDemandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.en_attente).count();
        long acceptees = toutesLesDemandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.acceptee).count();
        long refusees = toutesLesDemandes.stream()
                .filter(d -> d.getStatut() == DemandeStage.StatutDemande.refusee).count();
        long ceMois = toutesLesDemandes.stream()
                .filter(d -> d.getDateDemande() != null
                        && d.getDateDemande().getMonth() == java.time.LocalDate.now().getMonth()
                        && d.getDateDemande().getYear() == java.time.LocalDate.now().getYear())
                .count();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy 'à' HH'h'mm");
        List<Map<String, Object>> demandesJson = toutesLesDemandes.stream().map(d -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", d.getId());
            m.put("nomComplet", d.getPrenom() + " " + d.getNom());
            m.put("initiale", d.getPrenom().substring(0,1).toUpperCase() + d.getNom().substring(0,1).toUpperCase());
            m.put("email", d.getPrenom().toLowerCase() + "." + d.getNom().toLowerCase() + "@email.com");
            m.put("ecole", d.getEcole());
            m.put("filiere", d.getFiliere());
            m.put("niveau", d.getNiveau());
            m.put("dureeSouhaitee", d.getDureeSouhaitee());
            m.put("commentaire", d.getCommentaire());
            m.put("dateDemande", d.getDateDemande() != null ? sdf.format(java.sql.Timestamp.valueOf(d.getDateDemande())) : "—");
            m.put("statutCls", d.getStatut().name());
            m.put("statutLabel", d.getStatut() == DemandeStage.StatutDemande.en_attente ? "En attente"
                    : d.getStatut() == DemandeStage.StatutDemande.acceptee ? "Acceptée" : "Refusée");

            List<Document> docs = documentRepository.findByDemandeStageId(d.getId());
            List<Map<String, String>> docsJson = docs.stream().map(doc -> {
                Map<String, String> dm = new java.util.HashMap<>();
                dm.put("nom", doc.getNomFichier());
                dm.put("taille", "—");
                dm.put("dateDepot", doc.getDateDepot() != null
                        ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(java.sql.Timestamp.valueOf(doc.getDateDepot())) : "—");
                String ext = doc.getNomFichier() != null && doc.getNomFichier().contains(".")
                        ? doc.getNomFichier().substring(doc.getNomFichier().lastIndexOf(".")+1).toLowerCase() : "";
                if (ext.equals("pdf")) { dm.put("cls", "pdf"); dm.put("icon", "fa-solid fa-file-pdf"); }
                else if (ext.matches("doc|docx")) { dm.put("cls", "word"); dm.put("icon", "fa-solid fa-file-word"); }
                else if (ext.matches("png|jpg|jpeg|gif|svg")) { dm.put("cls", "image"); dm.put("icon", "fa-solid fa-file-image"); }
                else { dm.put("cls", "archive"); dm.put("icon", "fa-solid fa-file-zipper"); }
                return dm;
            }).collect(java.util.stream.Collectors.toList());

            m.put("documents", docsJson);
            m.put("documentsCount", docsJson.size());
            return m;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("demandes", toutesLesDemandes);
        model.addAttribute("totalDemandes", totalDemandes);
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("acceptees", acceptees);
        model.addAttribute("refusees", refusees);
        model.addAttribute("ceMois", ceMois);
        model.addAttribute("demandesJson", demandesJson);

        return "admin/demandes";
    }

    @GetMapping({"/stagiaires", "/stagiaire"})
    public String afficherStagiaires(Model model) {
        model.addAttribute("activePage", "stagiaires");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "A");

        List<Stagiaire> tousStagiaires = stagiaireRepository.findAll();
        List<Stage> tousStages = stageRepository.findAll();
        List<ServiceEntreprise> tousServices = serviceRepository.findAll();
        List<Encadreur> tousEncadreurs = encadreurRepository.findAll();

        long totalStagiaires = tousStagiaires.size();
        long enCours = tousStagiaires.stream()
                .filter(s -> s.getStatut() == Stagiaire.StatutStagiaire.actif).count();
        long termines = tousStagiaires.stream()
                .filter(s -> s.getStatut() == Stagiaire.StatutStagiaire.termine).count();
        long enAttente = tousStagiaires.stream()
                .filter(s -> s.getStatut() == Stagiaire.StatutStagiaire.actif
                        && stageRepository.findByStagiaireId(s.getId()).isEmpty()).count();
        long ceMois = tousStagiaires.stream()
                .filter(s -> s.getDateAdmission() != null
                        && s.getDateAdmission().getMonth() == java.time.LocalDate.now().getMonth()
                        && s.getDateAdmission().getYear() == java.time.LocalDate.now().getYear())
                .count();

        model.addAttribute("totalStagiaires", totalStagiaires);
        model.addAttribute("enCours", enCours);
        model.addAttribute("termines", termines);
        model.addAttribute("enAttente", enAttente);
        model.addAttribute("ceMois", ceMois);

        model.addAttribute("stagiaires", tousStagiaires);
        model.addAttribute("services", tousServices);
        model.addAttribute("encadreurs", tousEncadreurs);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        List<Map<String, Object>> stagiairesJson = tousStagiaires.stream().map(s -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            m.put("matricule", s.getMatricule());
            m.put("initiale", s.getUtilisateur().getPrenom().substring(0,1).toUpperCase()
                    + s.getUtilisateur().getNom().substring(0,1).toUpperCase());
            m.put("nomComplet", s.getUtilisateur().getPrenom() + " " + s.getUtilisateur().getNom());
            m.put("email", s.getUtilisateur().getEmail());
            m.put("telephone", s.getUtilisateur().getTelephone() != null ? s.getUtilisateur().getTelephone() : "—");
            m.put("adresse", s.getUtilisateur().getAdresse() != null ? s.getUtilisateur().getAdresse() : "—");
            m.put("statutCls", s.getStatut().name());
            m.put("statutLabel", s.getStatut() == Stagiaire.StatutStagiaire.actif ? "Actif"
                    : s.getStatut() == Stagiaire.StatutStagiaire.termine ? "Terminé" : "Abandonné");
            m.put("dateAdmission", s.getDateAdmission() != null ? sdf.format(java.sql.Date.valueOf(s.getDateAdmission())) : "—");

            if (s.getDemandeStage() != null) {
                m.put("universite", s.getDemandeStage().getEcole() != null ? s.getDemandeStage().getEcole() : "—");
                m.put("filiere", s.getDemandeStage().getFiliere() != null ? s.getDemandeStage().getFiliere() : "—");
                m.put("niveau", s.getDemandeStage().getNiveau() != null ? s.getDemandeStage().getNiveau() : "—");
            } else {
                m.put("universite", "—");
                m.put("filiere", "—");
                m.put("niveau", "—");
            }

            java.util.Optional<Stage> stageOpt = stageRepository.findByStagiaireId(s.getId());
            if (stageOpt.isPresent()) {
                Stage st = stageOpt.get();
                m.put("service", st.getService() != null ? st.getService().getNom() : "—");
                m.put("serviceId", st.getService() != null ? st.getService().getId().toString() : "");
                m.put("encadreur", st.getEncadreur() != null
                        ? st.getEncadreur().getUtilisateur().getPrenom() + " " + st.getEncadreur().getUtilisateur().getNom() : "—");
                m.put("encadreurId", st.getEncadreur() != null ? st.getEncadreur().getId().toString() : "");
                m.put("dateDebut", st.getDateDebut() != null ? sdf.format(java.sql.Date.valueOf(st.getDateDebut())) : "—");
                m.put("dateFin", st.getDateFin() != null ? sdf.format(java.sql.Date.valueOf(st.getDateFin())) : "—");
                m.put("duree", st.getDuree() != null ? st.getDuree() : "—");

                if (st.getDateFin() != null) {
                    long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), st.getDateFin());
                    if (joursRestants > 0) {
                        long mois = joursRestants / 30;
                        m.put("dureeRestante", mois > 0 ? mois + " mois" : joursRestants + " jours");
                    } else if (joursRestants == 0) {
                        m.put("dureeRestante", "Dernier jour");
                    } else {
                        m.put("dureeRestante", "Terminé");
                    }
                } else {
                    m.put("dureeRestante", "—");
                }

                if (st.getDateDebut() != null && st.getDateFin() != null) {
                    long total = java.time.temporal.ChronoUnit.DAYS.between(st.getDateDebut(), st.getDateFin());
                    long ecoule = java.time.temporal.ChronoUnit.DAYS.between(st.getDateDebut(), java.time.LocalDate.now());
                    int progression = total > 0 ? (int) Math.min(100, Math.max(0, ecoule * 100 / total)) : 0;
                    m.put("progression", progression);
                } else {
                    m.put("progression", 0);
                }
            } else {
                m.put("service", "—");
                m.put("serviceId", "");
                m.put("encadreur", "—");
                m.put("encadreurId", "");
                m.put("dateDebut", "—");
                m.put("dateFin", "—");
                m.put("duree", "—");
                m.put("dureeRestante", "—");
                m.put("progression", 0);
            }

            List<com.gestionstages.gestion_stages.entities.Document> docs = documentRepository.findAll().stream()
                    .filter(d -> d.getStage() != null && d.getStage().getId().equals(s.getId())
                            || (d.getDemandeStage() != null && s.getDemandeStage() != null
                                && d.getDemandeStage().getId().equals(s.getDemandeStage().getId())))
                    .collect(java.util.stream.Collectors.toList());
            List<Map<String, String>> docsJson = docs.stream().map(doc -> {
                Map<String, String> dm = new java.util.HashMap<>();
                dm.put("nom", doc.getNomFichier());
                dm.put("taille", "—");
                String ext = doc.getNomFichier() != null && doc.getNomFichier().contains(".")
                        ? doc.getNomFichier().substring(doc.getNomFichier().lastIndexOf(".")+1).toLowerCase() : "";
                if (ext.equals("pdf")) { dm.put("cls", "pdf"); dm.put("icon", "fa-solid fa-file-pdf"); }
                else if (ext.matches("doc|docx")) { dm.put("cls", "word"); dm.put("icon", "fa-solid fa-file-word"); }
                else if (ext.matches("png|jpg|jpeg|gif|svg")) { dm.put("cls", "image"); dm.put("icon", "fa-solid fa-file-image"); }
                else { dm.put("cls", "archive"); dm.put("icon", "fa-solid fa-file-zipper"); }
                return dm;
            }).collect(java.util.stream.Collectors.toList());
            m.put("documents", docsJson);
            m.put("documentsCount", docsJson.size());

            return m;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("stagiairesJson", stagiairesJson);
        return "admin/stagiaires";
    }

    @GetMapping("/encadreurs")
    public String afficherEncadreurs(Model model) {
        model.addAttribute("activePage", "encadreurs");
        model.addAttribute("encadreurs", encadreurRepository.findAll());
        return "admin/encadreurs";
    }

    @GetMapping("/messages")
    public String afficherMessages(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam(required = false) Integer convId, Model model) {
        model.addAttribute("activePage", "messages");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "A");
        model.addAttribute("notificationsCount", 0);
        model.addAttribute("recentNotifications", new ArrayList<>());
        Utilisateur currentUser = userDetails.getUtilisateur();
        model.addAttribute("user", currentUser);
        Integer userId = currentUser.getId();
        List<Conversation> conversations = conversationRepository.findByParticipantIdOrderByDernierMessageDesc(userId);
        model.addAttribute("conversations", conversations);
        Conversation active = null;
        if (convId != null) {
            active = conversationRepository.findById(convId).orElse(null);
        } else if (!conversations.isEmpty()) {
            active = conversations.get(0);
        }
        model.addAttribute("activeConversation", active);
        if (active != null) {
            model.addAttribute("messages", messageRepository.findByConversationIdOrderByDateEnvoiAsc(active.getId()));
        } else {
            model.addAttribute("messages", new ArrayList<>());
        }
        List<Utilisateur> contacts = utilisateurRepository.findAll();
        contacts.remove(currentUser);
        model.addAttribute("contacts", contacts);
        return "admin/messages";
    }

    @PostMapping("/messages/envoyer")
    public String envoyerMessageAdmin(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @RequestParam Integer convId,
                                      @RequestParam String contenu) {
        Conversation conv = conversationRepository.findById(convId).orElse(null);
        if (conv == null) return "redirect:/admin/messages";
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setExpediteur(userDetails.getUtilisateur());
        msg.setContenu(contenu);
        msg.setDateEnvoi(java.time.LocalDateTime.now());
        msg.setLu(false);
        conv.setDernierMessage(java.time.LocalDateTime.now());
        conversationRepository.save(conv);
        messageRepository.save(msg);
        return "redirect:/admin/messages?convId=" + convId;
    }

    @PostMapping("/messages/nouveau")
    public String nouvelleConversationAdmin(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestParam Integer destinataireId,
                                           @RequestParam String message) {
        Utilisateur currentUser = userDetails.getUtilisateur();
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
        if (destinataire == null) return "redirect:/admin/messages";
        List<Conversation> existing = conversationRepository.findByParticipantIdOrderByDernierMessageDesc(currentUser.getId());
        for (Conversation c : existing) {
            boolean hasDest = c.getParticipants().stream().anyMatch(p -> p.getId().equals(destinataireId));
            if (hasDest) {
                Message msg = new Message();
                msg.setConversation(c);
                msg.setExpediteur(currentUser);
                msg.setContenu(message);
                msg.setDateEnvoi(java.time.LocalDateTime.now());
                msg.setLu(false);
                c.setDernierMessage(java.time.LocalDateTime.now());
                conversationRepository.save(c);
                messageRepository.save(msg);
                return "redirect:/admin/messages?convId=" + c.getId();
            }
        }
        Conversation conv = new Conversation();
        conv.setSujet("Discussion avec " + destinataire.getPrenom() + " " + destinataire.getNom());
        conv.setDateCreation(java.time.LocalDateTime.now());
        conv.setDernierMessage(java.time.LocalDateTime.now());
        conv.getParticipants().add(currentUser);
        conv.getParticipants().add(destinataire);
        conv = conversationRepository.save(conv);
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setExpediteur(currentUser);
        msg.setContenu(message);
        msg.setDateEnvoi(java.time.LocalDateTime.now());
        msg.setLu(false);
        messageRepository.save(msg);
        return "redirect:/admin/messages?convId=" + conv.getId();
    }

    @GetMapping("/roles-permissions")
    public String afficherRolesPermissions(Model model) {
        model.addAttribute("activePage", "roles-permissions");
        return "admin/roles-permissions";
    }

    @GetMapping("/statistiques")
    public String afficherStatistiques(Model model) {
        model.addAttribute("activePage", "statistiques");
        model.addAttribute("nombreDemandes", demandeStageRepository.count());
        model.addAttribute("nombreStagiaires", stagiaireRepository.count());
        model.addAttribute("nombreEncadreurs", encadreurRepository.count());
        model.addAttribute("nombreStages", stageRepository.count());
        model.addAttribute("nombreTaches", tacheRepository.count());
        model.addAttribute("stagesEnCours", stageRepository.findByStatut(Stage.StatutStage.en_cours).size());
        model.addAttribute("stagesTermines", stageRepository.findByStatut(Stage.StatutStage.termine).size());

        return "admin/statistiques";
    }
}
