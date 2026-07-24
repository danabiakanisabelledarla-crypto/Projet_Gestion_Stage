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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "AD");

        // --- STATISTIQUES DEPUIS LA BASE ---
        long totalUsers = utilisateurRepository.count();
        long totalStagiaires = stagiaireRepository.count();
        long totalEncadreurs = encadreurRepository.count();
        long totalResp = utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size();
        long totalDocs = documentRepository.count();
        long totalLogs = activityLogRepository.count();

        model.addAttribute("statUtilisateurs", totalUsers);
        model.addAttribute("statStagiaires", totalStagiaires);
        model.addAttribute("statEncadreurs", totalEncadreurs);
        model.addAttribute("statResponsables", totalResp);
        model.addAttribute("statDocuments", totalDocs);
        model.addAttribute("statActivites", totalLogs);
        model.addAttribute("totalConnexions", totalLogs);

        // --- ACTIVITÉS RÉCENTES (depuis les logs) ---
        List<ActivityLog> recentLogs = activityLogRepository.findTop50ByOrderByDateActiviteDesc();
        List<Map<String, String>> activitesRecentes = recentLogs.stream().limit(5).map(a -> {
            Map<String, String> m = new HashMap<>();
            String action = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String icone;
            String couleur;
            if (action.contains("ajoute") || action.contains("cr")) {
                icone = "fa-solid fa-user-plus"; couleur = "bleu-clair";
            } else if (action.contains("sauvegarde")) {
                icone = "fa-solid fa-database"; couleur = "vert-clair";
            } else if (action.contains("supprim") || action.contains("desactive")) {
                icone = "fa-solid fa-times-circle"; couleur = "rouge-clair";
            } else if (action.contains("modifie")) {
                icone = "fa-solid fa-sliders-h"; couleur = "orange-clair";
            } else {
                icone = "fa-solid fa-bolt"; couleur = "bleu";
            }
            m.put("icone", icone);
            m.put("couleur", couleur);
            m.put("message", a.getAction() != null ? a.getAction() : "Action");
            if (a.getDateActivite() != null) {
                long minutes = java.time.Duration.between(a.getDateActivite(), java.time.LocalDateTime.now()).toMinutes();
                if (minutes < 1) m.put("temps", "À l'instant");
                else if (minutes < 60) m.put("temps", "Il y a " + minutes + " min");
                else if (minutes < 1440) m.put("temps", "Il y a " + (minutes / 60) + "h");
                else m.put("temps", "Il y a " + (minutes / 1440) + " jours");
            } else {
                m.put("temps", "—");
            }
            return m;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("activitesRecentes", activitesRecentes);

        // --- STATUT SYSTÈME ---
        List<Map<String, String>> statutSysteme = List.of(
            Map.of("icone", "fa-solid fa-server", "nom", "Serveur", "statut", "Opérationnel", "couleur", "verte"),
            Map.of("icone", "fa-solid fa-database", "nom", "Base de données", "statut", "À jour", "couleur", "verte"),
            Map.of("icone", "fa-solid fa-hdd", "nom", "Stockage", "statut", totalDocs * 5 / 32 + "% utilisé", "couleur", "orange"),
            Map.of("icone", "fa-solid fa-cloud-upload-alt", "nom", "Sauvegarde", "statut", "Aucun incident", "couleur", "verte"),
            Map.of("icone", "fa-solid fa-shield-alt", "nom", "Sécurité", "statut", "Protégé", "couleur", "verte")
        );
        model.addAttribute("statutSysteme", statutSysteme);

        // --- UTILISATEURS RÉCENTS (depuis la base) ---
        List<Map<String, String>> utilisateursRecents = utilisateurRepository.findAll().stream()
            .sorted((a,b) -> b.getDateCreation().compareTo(a.getDateCreation()))
            .limit(3)
            .map(u -> {
                Map<String, String> m = new HashMap<>();
                m.put("nom", u.getPrenom() + " " + u.getNom());
                String role = u.getRole().getLibelle().toLowerCase();
                m.put("role", role.contains("encadreur") ? "encadreur" : role.contains("responsable") ? "responsable" : "stagiaire");
                if (u.getDateCreation() != null) {
                    long minutes = java.time.Duration.between(u.getDateCreation(), java.time.LocalDateTime.now()).toMinutes();
                    if (minutes < 60) m.put("temps", minutes + " min");
                    else if (minutes < 1440) m.put("temps", (minutes / 60) + "h");
                    else m.put("temps", (minutes / 1440) + "j");
                } else {
                    m.put("temps", "—");
                }
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("utilisateursRecents", utilisateursRecents);

        // --- DONNÉES POUR LES GRAPHIQUES ---
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE");
        List<String> joursSemaine = List.of("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim");
        model.addAttribute("chartDays", joursSemaine);

        List<Integer> connexionsValues = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate day = java.time.LocalDate.now().minusDays(i);
            long count = recentLogs.stream()
                .filter(a -> a.getDateActivite() != null && a.getDateActivite().toLocalDate().equals(day))
                .count();
            connexionsValues.add(0, (int) count);
        }
        model.addAttribute("chartConnexionsValues", connexionsValues);
        int maxVal = connexionsValues.stream().max(Integer::compare).orElse(1);
        int step1 = connexionsValues.size() > 1 ? 680 / (connexionsValues.size() - 1) : 680;
        int step2 = connexionsValues.size() > 1 ? 700 / (connexionsValues.size() - 1) : 700;
        model.addAttribute("chartConnexionsMax", maxVal == 0 ? 1 : maxVal);
        model.addAttribute("chartConnexionsStep1", step1);
        model.addAttribute("chartConnexionsStep2", step2);
        model.addAttribute("chartConnexionsPointsCompact",
                buildDashboardPoints(connexionsValues, 680, 170, 120));
        model.addAttribute("chartConnexionsPoints",
                buildDashboardPoints(connexionsValues, 700, 190, 150));

        // Répartition des utilisateurs
        long adminCount = utilisateurRepository.findByRole_Libelle("ADMINISTRATEUR").size();
        long respCount = utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size();
        long encadCount = utilisateurRepository.findByRole_Libelle("ENCADREUR").size();
        long stagCount = utilisateurRepository.findByRole_Libelle("STAGIAIRE").size();
        long denom = Math.max(1, totalUsers);
        model.addAttribute("distStagiaires", stagCount);
        model.addAttribute("distEncadreurs", encadCount);
        model.addAttribute("distResponsables", respCount);
        model.addAttribute("distAdmins", adminCount);
        model.addAttribute("pctStagiaires", stagCount * 100 / denom);
        model.addAttribute("pctEncadreurs", encadCount * 100 / denom);
        model.addAttribute("pctResponsables", respCount * 100 / denom);
        model.addAttribute("pctAdmins", adminCount * 100 / denom);
        model.addAttribute("donutStagiaires", buildDonutSegment(stagCount, denom));
        model.addAttribute("donutEncadreurs", buildDonutSegment(encadCount, denom));
        model.addAttribute("donutResponsables", buildDonutSegment(respCount, denom));
        model.addAttribute("donutAdmins", buildDonutSegment(adminCount, denom));
        model.addAttribute("donutOffsetEncadreurs", buildDonutOffset(stagCount, denom));
        model.addAttribute("donutOffsetResponsables", buildDonutOffset(stagCount + encadCount, denom));
        model.addAttribute("donutOffsetAdmins", buildDonutOffset(stagCount + encadCount + respCount, denom));

        return "admin/dashboard";
    }

    private String buildDashboardPoints(List<Integer> values, int width, int baseline, int chartHeight) {
        StringBuilder sb = new StringBuilder();
        int max = values.stream().max(Integer::compare).orElse(1);
        if (max == 0) max = 1;
        double scale = (double) chartHeight / max;
        int spacing = width / Math.max(1, values.size() - 1);
        for (int i = 0; i < values.size(); i++) {
            int x = 50 + i * spacing;
            int y = baseline - (int) (values.get(i) * scale);
            sb.append(x).append(",").append(y).append(" ");
        }
        return sb.toString().trim();
    }

    private String buildDonutSegment(long value, long total) {
        double segment = value * 99.9 / Math.max(1, total);
        return String.format(java.util.Locale.ROOT, "%.2f %.2f", segment, 99.9 - segment);
    }

    private String buildDonutOffset(long precedingValues, long total) {
        double offset = precedingValues * 99.9 / Math.max(1, total);
        return String.format(java.util.Locale.ROOT, "-%.2f", offset);
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
