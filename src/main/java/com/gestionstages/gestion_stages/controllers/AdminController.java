package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;
import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.gestionstages.gestion_stages.security.CustomUserDetails;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final LivrableRepository livrableRepository;
    private final ProjetRepository projetRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final com.gestionstages.gestion_stages.services.PermissionService permissionService;
    private final com.gestionstages.gestion_stages.services.MessagingService messagingService;

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
                            MessageRepository messageRepository,
                            RoleRepository roleRepository,
                            PermissionRepository permissionRepository,
                             LivrableRepository livrableRepository,
                             ProjetRepository projetRepository,
                            PasswordEncoder passwordEncoder,
                            EmailService emailService,
                            com.gestionstages.gestion_stages.services.PermissionService permissionService,
                            com.gestionstages.gestion_stages.services.MessagingService messagingService) {
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
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.livrableRepository = livrableRepository;
        this.projetRepository = projetRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.permissionService = permissionService;
        this.messagingService = messagingService;
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
            m.put("email", extraireEmailCandidat(d));
            m.put("utilisateurId", utilisateurRepository.findByEmail(extraireEmailCandidat(d))
                    .map(Utilisateur::getId).orElse(null));
            m.put("ecole", d.getEcole());
            m.put("filiere", d.getFiliere());
            m.put("niveau", d.getNiveau());
            m.put("dureeSouhaitee", d.getDureeSouhaitee());
            m.put("commentaire", d.getCommentaire());
            m.put("motifRefus", d.getMotifRefus());
            m.put("dateDemande", d.getDateDemande() != null ? sdf.format(java.sql.Timestamp.valueOf(d.getDateDemande())) : "—");
            m.put("statutCls", d.getStatut().name());
            m.put("statutLabel", d.getStatut() == DemandeStage.StatutDemande.en_attente ? "En attente"
                    : d.getStatut() == DemandeStage.StatutDemande.acceptee ? "Acceptée" : "Refusée");

            List<Document> docs = documentRepository.findByDemandeStageId(d.getId());
            List<Map<String, String>> docsJson = docs.stream().map(doc -> {
                Map<String, String> dm = new java.util.HashMap<>();
                dm.put("id", doc.getId().toString());
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
        model.addAttribute("emailsDemandes", toutesLesDemandes.stream().collect(Collectors.toMap(
                DemandeStage::getId,
                this::extraireEmailCandidat
        )));

        return "admin/demandes";
    }

    @PostMapping("/demandes/{id}/accepter")
    public String accepterDemande(@PathVariable Integer id,
                                  @RequestParam String email,
                                  @RequestParam String motDePasse,
                                  RedirectAttributes redirectAttributes) {
        Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
        if (demandeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Demande introuvable.");
            return "redirect:/admin/demandes";
        }

        DemandeStage demande = demandeOpt.get();
        String emailNormalise = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (emailNormalise.isBlank() || motDePasse == null || motDePasse.length() < 6) {
            redirectAttributes.addFlashAttribute("erreur", "L'adresse email et un mot de passe d'au moins 6 caractères sont requis.");
            return "redirect:/admin/demandes";
        }
        if (stagiaireRepository.findByDemandeStageId(id).isPresent()) {
            redirectAttributes.addFlashAttribute("erreur", "Un compte stagiaire existe déjà pour cette demande.");
            return "redirect:/admin/demandes";
        }
        if (utilisateurRepository.existsByEmail(emailNormalise)) {
            redirectAttributes.addFlashAttribute("erreur", "Cette adresse email est déjà utilisée.");
            return "redirect:/admin/demandes";
        }

        Role roleStagiaire = roleRepository.findByLibelle("STAGIAIRE")
                .orElseThrow(() -> new IllegalStateException("Le rôle STAGIAIRE est introuvable."));
        Utilisateur utilisateur = new Utilisateur(
                roleStagiaire,
                demande.getNom(),
                demande.getPrenom(),
                emailNormalise,
                passwordEncoder.encode(motDePasse)
        );
        utilisateurRepository.save(utilisateur);

        String matricule = genererMatriculeStagiaire();
        stagiaireRepository.save(new Stagiaire(utilisateur, demande, matricule, LocalDate.now()));
    demande.setStatut(DemandeStage.StatutDemande.acceptee);
    demande.setMotifRefus(null);
    demande.setEmail(emailNormalise);
    demandeStageRepository.save(demande);

    boolean emailEnvoye = emailService.envoyerConfirmationAdmission(
            emailNormalise,
            demande.getPrenom() + " " + demande.getNom(),
            emailNormalise,
            motDePasse
    );

    if (emailEnvoye) {
        redirectAttributes.addFlashAttribute("succes",
                "Demande acceptée, compte " + matricule + " créé et notification envoyée à " + emailNormalise + ".");
    } else {
        redirectAttributes.addFlashAttribute("erreur",
                "La demande et le compte ont été enregistrés, mais l'e-mail n'a pas pu être envoyé à "
                        + emailNormalise + ". Vérifiez la configuration SMTP.");
    }
        return "redirect:/admin/demandes";
    }

    @PostMapping("/demandes/{id}/refuser")
    public String refuserDemande(@PathVariable Integer id,
                                 @RequestParam String motif,
                                 RedirectAttributes redirectAttributes) {
        Optional<DemandeStage> demandeOpt = demandeStageRepository.findById(id);
        if (demandeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Demande introuvable.");
            return "redirect:/admin/demandes";
        }
        if (motif == null || motif.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("erreur", "Veuillez préciser un motif de refus d'au moins 10 caractères.");
            return "redirect:/admin/demandes";
        }

        DemandeStage demande = demandeOpt.get();
        demande.setStatut(DemandeStage.StatutDemande.refusee);
        demande.setMotifRefus(motif.trim());
        demandeStageRepository.save(demande);
    String destinataire = extraireEmailCandidat(demande);
    boolean emailEnvoye = emailService.envoyerRefusDemande(
            destinataire,
            demande.getPrenom() + " " + demande.getNom(),
            motif.trim()
    );

    if (emailEnvoye) {
        redirectAttributes.addFlashAttribute("succes",
                "Demande refusée et notification envoyée à " + destinataire + ".");
    } else {
        redirectAttributes.addFlashAttribute("erreur",
                "Le refus a été enregistré, mais l'e-mail n'a pas pu être envoyé à "
                        + destinataire + ". Vérifiez la configuration SMTP.");
    }
        return "redirect:/admin/demandes";
    }

    @GetMapping("/demandes/exporter")
    public ResponseEntity<byte[]> exporterDemandes() {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID;Nom;Prénom;Email;École;Filière;Niveau;Durée souhaitée;Date;Statut;Motif du refus\n");
        for (DemandeStage demande : demandeStageRepository.findAll()) {
            csv.append(valeurCsv(demande.getId())).append(';')
                    .append(valeurCsv(demande.getNom())).append(';')
                    .append(valeurCsv(demande.getPrenom())).append(';')
                    .append(valeurCsv(extraireEmailCandidat(demande))).append(';')
                    .append(valeurCsv(demande.getEcole())).append(';')
                    .append(valeurCsv(demande.getFiliere())).append(';')
                    .append(valeurCsv(demande.getNiveau())).append(';')
                    .append(valeurCsv(demande.getDureeSouhaitee())).append(';')
                    .append(valeurCsv(demande.getDateDemande())).append(';')
                    .append(valeurCsv(demande.getStatut())).append(';')
                    .append(valeurCsv(demande.getMotifRefus())).append('\n');
        }
        byte[] contenu = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"demandes-stage.xls\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel;charset=UTF-8"))
                .contentLength(contenu.length)
                .body(contenu);
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
        LocalDate aujourdHui = LocalDate.now();
        for (Stage stage : tousStages) {
            if (stage.getDateFin() != null && !stage.getDateFin().isAfter(aujourdHui)
                    && stage.getStatut() != Stage.StatutStage.termine) {
                stage.setStatut(Stage.StatutStage.termine);
                stageRepository.save(stage);
                stage.getStagiaire().setStatut(Stagiaire.StatutStagiaire.termine);
                stagiaireRepository.save(stage.getStagiaire());
            }
        }
        Map<Integer, Stage> stagesParStagiaire = tousStages.stream()
                .collect(Collectors.toMap(stage -> stage.getStagiaire().getId(), stage -> stage, (a, b) -> a));
        Map<Integer, Map<String, LocalDate>> datesParStagiaire = new HashMap<>();
        for (Stagiaire stagiaire : tousStagiaires) {
            Stage stage = stagesParStagiaire.get(stagiaire.getId());
            LocalDate debut = stage != null ? stage.getDateDebut() : stagiaire.getDateAdmission();
            LocalDate fin = stage != null ? stage.getDateFin()
                    : calculerDateFin(debut, stagiaire.getDemandeStage() != null
                            ? stagiaire.getDemandeStage().getDureeSouhaitee() : null);
            Map<String, LocalDate> periode = new HashMap<>();
            periode.put("debut", debut);
            periode.put("fin", fin);
            datesParStagiaire.put(stagiaire.getId(), periode);
        }

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
        model.addAttribute("projets", projetRepository.findAll());
        model.addAttribute("stagesParStagiaire", stagesParStagiaire);
        model.addAttribute("datesParStagiaire", datesParStagiaire);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
        List<Map<String, Object>> stagiairesJson = tousStagiaires.stream().map(s -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            m.put("prenom", s.getUtilisateur().getPrenom());
            m.put("nom", s.getUtilisateur().getNom());
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
            Stage stageStagiaire = stageOpt.orElse(null);
            if (stageOpt.isPresent()) {
                Stage st = stageStagiaire;
                m.put("stageId", st.getId());
                m.put("service", st.getService() != null ? st.getService().getNom() : "—");
                m.put("serviceId", st.getService() != null ? st.getService().getId().toString() : "");
                m.put("encadreur", st.getEncadreur() != null
                        ? st.getEncadreur().getUtilisateur().getPrenom() + " " + st.getEncadreur().getUtilisateur().getNom() : "—");
                m.put("encadreurId", st.getEncadreur() != null ? st.getEncadreur().getId().toString() : "");
                m.put("dateDebut", st.getDateDebut() != null ? sdf.format(java.sql.Date.valueOf(st.getDateDebut())) : "—");
                m.put("dateFin", st.getDateFin() != null ? sdf.format(java.sql.Date.valueOf(st.getDateFin())) : "—");
                m.put("dateDebutIso", st.getDateDebut() != null ? st.getDateDebut().toString() : "");
                m.put("dateFinIso", st.getDateFin() != null ? st.getDateFin().toString() : "");
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
                m.put("stageId", null);
                m.put("service", "—");
                m.put("serviceId", "");
                m.put("encadreur", "—");
                m.put("encadreurId", "");
                m.put("dateDebut", "—");
                m.put("dateFin", "—");
                m.put("dateDebutIso", "");
                m.put("dateFinIso", "");
                m.put("duree", "—");
                m.put("dureeRestante", "—");
                m.put("progression", 0);
            }

            List<Document> dossierDocuments = s.getDemandeStage() == null
                    ? List.of()
                    : documentRepository.findByDemandeStageId(s.getDemandeStage().getId());
            List<Map<String, String>> docsJson = dossierDocuments.stream().map(doc -> {
                Map<String, String> dm = new java.util.HashMap<>();
                dm.put("id", doc.getId().toString());
                dm.put("nom", doc.getNomFichier());
                dm.put("taille", doc.getTailleOctets() != null
                        ? String.format(Locale.FRANCE, "%.1f Mo", doc.getTailleOctets() / 1048576.0)
                        : "—");
                dm.put("date", doc.getDateDepot() != null
                        ? doc.getDateDepot().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        : "—");
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

            List<Livrable> livrables = new ArrayList<>();
            if (stageStagiaire != null) {
                livrables.addAll(livrableRepository.findByStageId(stageStagiaire.getId()));
                for (Tache tache : tacheRepository.findByStageId(stageStagiaire.getId())) {
                    for (Livrable livrable : livrableRepository.findByTacheId(tache.getId())) {
                        if (livrables.stream().noneMatch(item -> item.getId().equals(livrable.getId()))) {
                            livrables.add(livrable);
                        }
                    }
                }
            }
            List<Map<String, String>> livrablesJson = livrables.stream().map(livrable -> {
                Map<String, String> lm = new HashMap<>();
                lm.put("id", livrable.getId().toString());
                lm.put("titre", livrable.getTitre());
                lm.put("categorie", livrable.getCategorie() != null ? livrable.getCategorie() : "Autre");
                lm.put("statut", livrable.getStatut().name());
                lm.put("fichier", livrable.getFichier());
                lm.put("date", livrable.getDateDepot() != null
                        ? livrable.getDateDepot().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—");
                lm.put("taille", livrable.getTailleOctets() != null
                        ? String.format(Locale.FRANCE, "%.1f Mo", livrable.getTailleOctets() / 1048576.0)
                        : "—");
                return lm;
            }).collect(Collectors.toList());
            m.put("livrables", livrablesJson);
            m.put("livrablesCount", livrablesJson.size());

            List<Map<String, String>> tachesJson = stageStagiaire == null
                    ? List.of()
                    : tacheRepository.findByStageId(stageStagiaire.getId()).stream().map(tache -> {
                        Map<String, String> tm = new HashMap<>();
                        tm.put("titre", tache.getTitre());
                        tm.put("description", tache.getDescription() != null ? tache.getDescription() : "");
                        tm.put("statut", tache.getStatut().name());
                        tm.put("dateLimite", tache.getDateLimite() != null
                                ? tache.getDateLimite().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                : "—");
                        return tm;
                    }).collect(Collectors.toList());
            m.put("taches", tachesJson);
            m.put("tachesCount", tachesJson.size());

            return m;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("stagiairesJson", stagiairesJson);
        return "admin/stagiaires";
    }

    private LocalDate calculerDateFin(LocalDate dateDebut, String duree) {
        if (dateDebut == null || duree == null || duree.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(\\d+)\\s*(jour|jours|semaine|semaines|mois|an|ans|année|années)",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(duree.trim());
        if (!matcher.find()) {
            return null;
        }
        long valeur = Long.parseLong(matcher.group(1));
        String unite = matcher.group(2).toLowerCase(java.util.Locale.FRENCH);
        if (unite.startsWith("jour")) return dateDebut.plusDays(valeur);
        if (unite.startsWith("semaine")) return dateDebut.plusWeeks(valeur);
        if (unite.equals("mois")) return dateDebut.plusMonths(valeur);
        return dateDebut.plusYears(valeur);
    }

    @PostMapping("/stagiaires/{id}/modifier")
    public String modifierFicheStagiaire(@PathVariable Integer id,
                                         @RequestParam String prenom,
                                         @RequestParam String nom,
                                         @RequestParam String email,
                                         @RequestParam(required = false) String telephone,
                                         @RequestParam(required = false) String adresse,
                                         @RequestParam(required = false) Integer serviceId,
                                         @RequestParam(required = false) Integer encadreurId,
                                         @RequestParam(required = false) String dateDebut,
                                         @RequestParam(required = false) String dateFin,
                                         RedirectAttributes redirectAttributes) {
        Optional<Stagiaire> stagiaireOpt = stagiaireRepository.findById(id);
        if (stagiaireOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Stagiaire introuvable.");
            return "redirect:/admin/stagiaires";
        }

        Stagiaire stagiaire = stagiaireOpt.get();
        Utilisateur utilisateur = stagiaire.getUtilisateur();
        String emailNormalise = email.trim().toLowerCase(Locale.ROOT);
        Optional<Utilisateur> compteExistant = utilisateurRepository.findByEmail(emailNormalise);
        if (compteExistant.isPresent() && !compteExistant.get().getId().equals(utilisateur.getId())) {
            redirectAttributes.addFlashAttribute("erreur", "Cette adresse email est déjà utilisée.");
            return "redirect:/admin/stagiaires";
        }
        utilisateur.setPrenom(prenom.trim());
        utilisateur.setNom(nom.trim());
        utilisateur.setEmail(emailNormalise);
        utilisateur.setTelephone(telephone);
        utilisateur.setAdresse(adresse);
        utilisateurRepository.save(utilisateur);

        stageRepository.findByStagiaireId(id).ifPresent(stage -> {
            if (serviceId != null) serviceRepository.findById(serviceId).ifPresent(stage::setService);
            if (encadreurId != null) encadreurRepository.findById(encadreurId).ifPresent(stage::setEncadreur);
            if (dateDebut != null && !dateDebut.isBlank()) stage.setDateDebut(LocalDate.parse(dateDebut));
            if (dateFin != null && !dateFin.isBlank()) stage.setDateFin(LocalDate.parse(dateFin));
            if (stage.getDateDebut() != null && stage.getDateFin() != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(stage.getDateDebut(), stage.getDateFin());
                stage.setDuree(jours + " jours");
            }
            stageRepository.save(stage);
        });

        redirectAttributes.addFlashAttribute("succes", "La fiche du stagiaire a été mise à jour.");
        return "redirect:/admin/stagiaires";
    }

    @PostMapping("/stagiaires/{id}/acces")
    public String modifierAccesStagiaire(@PathVariable Integer id,
                                         @RequestParam boolean actif,
                                         RedirectAttributes redirectAttributes) {
        stagiaireRepository.findById(id).ifPresent(stagiaire -> {
            Utilisateur utilisateur = stagiaire.getUtilisateur();
            utilisateur.setStatut(actif
                    ? Utilisateur.StatutUtilisateur.actif
                    : Utilisateur.StatutUtilisateur.inactif);
            utilisateurRepository.save(utilisateur);
            emailService.envoyerStatutCompte(
                    utilisateur.getEmail(),
                    utilisateur.getPrenom() + " " + utilisateur.getNom(),
                    !actif);
        });
        redirectAttributes.addFlashAttribute("succes",
                actif ? "Le compte du stagiaire a été débloqué." : "Le compte du stagiaire a été bloqué.");
        return "redirect:/admin/stagiaires";
    }

    @PostMapping("/stagiaires/{id}/affecter")
    public String affecterStagiaire(@PathVariable Integer id,
                                    @RequestParam Integer encadreurId,
                                    @RequestParam Integer serviceId,
                                    @RequestParam(required = false) Integer projetId,
                                    @RequestParam String dateDebut,
                                    @RequestParam String dateFin,
                                    RedirectAttributes redirectAttributes) {
        Optional<Stagiaire> stagiaireOpt = stagiaireRepository.findById(id);
        Optional<Encadreur> encadreurOpt = encadreurRepository.findById(encadreurId);
        Optional<ServiceEntreprise> serviceOpt = serviceRepository.findById(serviceId);
        if (stagiaireOpt.isEmpty() || encadreurOpt.isEmpty() || serviceOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "L'affectation est incomplète.");
            return "redirect:/admin/stagiaires";
        }
        LocalDate debut = LocalDate.parse(dateDebut);
        LocalDate fin = LocalDate.parse(dateFin);
        if (!fin.isAfter(debut)) {
            redirectAttributes.addFlashAttribute("erreur",
                    "La date de fin doit être postérieure à la date de début.");
            return "redirect:/admin/stagiaires";
        }
        Stage stage = stageRepository.findByStagiaireId(id).orElseGet(Stage::new);
        stage.setStagiaire(stagiaireOpt.get());
        stage.setEncadreur(encadreurOpt.get());
        stage.setService(serviceOpt.get());
        stage.setProjet(projetId == null ? null : projetRepository.findById(projetId).orElse(null));
        stage.setDateDebut(debut);
        stage.setDateFin(fin);
        stage.setDuree(ChronoUnit.DAYS.between(debut, fin) + " jours");
        stage.setStatut(Stage.StatutStage.en_cours);
        if (stage.getNumeroStage() == null || stage.getNumeroStage().isBlank()) {
            stage.setNumeroStage("STG-ADM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        stageRepository.save(stage);
        Stagiaire stagiaire = stagiaireOpt.get();
        stagiaire.setStatut(Stagiaire.StatutStagiaire.actif);
        stagiaireRepository.save(stagiaire);
        redirectAttributes.addFlashAttribute("succes",
                "Le stagiaire a été affecté avec succès.");
        return "redirect:/admin/stagiaires";
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
        messagingService.preparerModele(
                model, userDetails.getUtilisateur(), convId, "/admin/messages", "Administrateur");
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
        List<Role> roles = roleRepository.findAll();
        Set<String> rolesSysteme = Set.of("ADMINISTRATEUR", "RESPONSABLE_STAGE", "ENCADREUR", "STAGIAIRE");
        Map<String, Integer> utilisateursParRole = roles.stream().collect(Collectors.toMap(
                Role::getLibelle,
                role -> utilisateurRepository.findByRole_Libelle(role.getLibelle()).size()
        ));
        model.addAttribute("roles", roles);
        model.addAttribute("rolesParLibelle", roles.stream()
                .collect(Collectors.toMap(Role::getLibelle, role -> role)));
        model.addAttribute("permissions", permissionRepository.findAll());
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
        model.addAttribute("utilisateursParRole", utilisateursParRole);
        model.addAttribute("rolesPersonnalises", roles.stream()
                .filter(role -> !rolesSysteme.contains(role.getLibelle()))
                .toList());
        model.addAttribute("nombreRoles", roles.size());
        model.addAttribute("nombrePermissions", permissionRepository.count());
        model.addAttribute("nombrePermissionsActives", roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getId)
                .distinct()
                .count());
        model.addAttribute("nombreUtilisateursRoles", utilisateurRepository.count());
        model.addAttribute("nombreAdministrateurs", utilisateurRepository.findByRole_Libelle("ADMINISTRATEUR").size());
        model.addAttribute("nombreResponsables", utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size());
        model.addAttribute("nombreEncadreursRole", utilisateurRepository.findByRole_Libelle("ENCADREUR").size());
        model.addAttribute("nombreStagiairesRole", utilisateurRepository.findByRole_Libelle("STAGIAIRE").size());
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "A");
        return "admin/roles-permissions";
    }

    @PostMapping("/roles-permissions/ajouter")
    public String ajouterRole(@RequestParam String libelle,
                              @RequestParam(required = false) String description,
                              @RequestParam(defaultValue = "STAGIAIRE") String espace,
                              @RequestParam(required = false) List<Integer> permissionIds,
                              RedirectAttributes redirectAttributes) {
        String roleLibelle = libelle == null ? "" : java.text.Normalizer
                .normalize(libelle.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (roleLibelle.isBlank()) {
            redirectAttributes.addFlashAttribute("erreur", "Le nom du rôle est obligatoire.");
            return "redirect:/admin/roles-permissions";
        }
        if (roleRepository.findByLibelle(roleLibelle).isPresent()) {
            redirectAttributes.addFlashAttribute("erreur", "Ce rôle existe déjà.");
            return "redirect:/admin/roles-permissions";
        }
        Role role = new Role(roleLibelle, description);
        Set<String> espacesAutorises = Set.of("RESPONSABLE_STAGE", "ENCADREUR", "STAGIAIRE");
        role.setEspace(espacesAutorises.contains(espace) ? espace : "STAGIAIRE");
        if (permissionIds != null) {
            role.setPermissions(new LinkedHashSet<>(permissionRepository.findAllById(permissionIds)));
        }
        roleRepository.save(role);
        redirectAttributes.addFlashAttribute("succes", "Le rôle " + roleLibelle + " a été créé.");
        return "redirect:/admin/roles-permissions";
    }

    @PostMapping("/roles-permissions/attribuer")
    public String attribuerRole(@RequestParam Integer utilisateurId,
                                @RequestParam Integer roleId,
                                RedirectAttributes redirectAttributes) {
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findById(utilisateurId);
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (utilisateurOpt.isEmpty() || roleOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Utilisateur ou rôle introuvable.");
            return "redirect:/admin/roles-permissions";
        }

        Utilisateur utilisateur = utilisateurOpt.get();
        Role ancienRole = utilisateur.getRole();
        Role nouveauRole = roleOpt.get();
        utilisateur.setRole(nouveauRole);
        utilisateurRepository.save(utilisateur);
        assurerProfilPourEspace(utilisateur, nouveauRole.getEspaceEffectif());

        boolean emailEnvoye = emailService.envoyerChangementRole(
                utilisateur.getEmail(),
                utilisateur.getPrenom() + " " + utilisateur.getNom(),
                utilisateur.getTelephone(),
                nouveauRole.getLibelle(),
                nouveauRole.getPermissions().stream().map(Permission::getNom).toList()
        );

        String action = ancienRole != null && ancienRole.getId().equals(nouveauRole.getId())
                ? "a été confirmé"
                : "a été changé de " + (ancienRole == null ? "sans rôle" : ancienRole.getLibelle())
                        + " vers " + nouveauRole.getLibelle();
        if (emailEnvoye) {
            redirectAttributes.addFlashAttribute("succes",
                    "Le rôle de " + utilisateur.getPrenom() + " " + utilisateur.getNom() + " " + action
                            + ". Un e-mail récapitulatif a été envoyé.");
        } else {
            redirectAttributes.addFlashAttribute("erreur",
                    "Le rôle et les permissions ont bien été mis à jour, mais l'e-mail n'a pas pu être envoyé à "
                            + utilisateur.getEmail() + ". Vérifiez la configuration SMTP.");
        }
        return "redirect:/admin/roles-permissions";
    }

    @PostMapping("/roles-permissions/roles/{roleId}/permissions")
    public String modifierPermissionsRole(@PathVariable Integer roleId,
                                           @RequestParam(required = false) List<Integer> permissionIds,
                                           RedirectAttributes redirectAttributes) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Rôle introuvable.");
            return "redirect:/admin/roles-permissions";
        }
        Role role = roleOpt.get();
        role.setPermissions(permissionIds == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(permissionRepository.findAllById(permissionIds)));
        roleRepository.save(role);
        
        // Synchroniser avec les mappings pour éviter les duplications
        permissionService.synchroniserRoleAvecMappings(role);
        
        redirectAttributes.addFlashAttribute("succes",
                "Les permissions du rôle " + role.getLibelle() + " ont été mises à jour.");
        return "redirect:/admin/roles-permissions";
    }

    @PostMapping("/roles-permissions/permissions/ajouter")
    public String ajouterPermission(@RequestParam String nom,
                                     @RequestParam(required = false) String description,
                                     RedirectAttributes redirectAttributes) {
        if (nom == null || nom.isBlank()) {
            redirectAttributes.addFlashAttribute("erreur", "Le nom de la permission est obligatoire.");
            return "redirect:/admin/roles-permissions";
        }
        String code = normaliserCode(nom);
        if (permissionRepository.findByCode(code).isPresent()
                || permissionRepository.existsByNomIgnoreCase(nom.trim())) {
            redirectAttributes.addFlashAttribute("erreur", "Cette permission existe déjà.");
            return "redirect:/admin/roles-permissions";
        }
        permissionRepository.save(new Permission(code, nom.trim(), description));
        redirectAttributes.addFlashAttribute("succes", "La permission " + nom.trim() + " a été ajoutée.");
        return "redirect:/admin/roles-permissions";
    }

    @PostMapping("/roles-permissions/permissions/{permissionId}/supprimer")
    public String supprimerPermission(@PathVariable Integer permissionId,
                                       RedirectAttributes redirectAttributes) {
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        if (permissionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("erreur", "Permission introuvable.");
            return "redirect:/admin/roles-permissions";
        }
        Permission permission = permissionOpt.get();
        roleRepository.findAll().forEach(role -> {
            if (role.getPermissions().removeIf(item -> item.getId().equals(permissionId))) {
                roleRepository.save(role);
            }
        });
        permissionRepository.delete(permission);
        redirectAttributes.addFlashAttribute("succes",
                "La permission " + permission.getNom() + " a été supprimée.");
        return "redirect:/admin/roles-permissions";
    }

    private String normaliserCode(String valeur) {
        return java.text.Normalizer.normalize(valeur.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private void assurerProfilPourEspace(Utilisateur utilisateur, String espace) {
        if ("ENCADREUR".equals(espace)
                && encadreurRepository.findByUtilisateurId(utilisateur.getId()).isEmpty()) {
            encadreurRepository.save(new Encadreur(
                    utilisateur,
                    "À renseigner",
                    "Encadreur"
            ));
        }

        if ("STAGIAIRE".equals(espace)
                && stagiaireRepository.findByUtilisateurId(utilisateur.getId()).isEmpty()) {
            DemandeStage demande = new DemandeStage(
                    utilisateur.getNom(),
                    utilisateur.getPrenom(),
                    "À renseigner",
                    "À renseigner",
                    "À renseigner",
                    "À renseigner"
            );
            demande.setEmail(utilisateur.getEmail());
            demande.setCommentaire("Profil créé lors d'un changement de rôle");
            demande.setStatut(DemandeStage.StatutDemande.acceptee);
            demandeStageRepository.save(demande);

            String matricule = "STG-ROLE-" + utilisateur.getId() + "-" + System.currentTimeMillis();
            stagiaireRepository.save(new Stagiaire(
                    utilisateur,
                    demande,
                    matricule,
                    LocalDate.now()
            ));
        }
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

private String extraireEmailCandidat(DemandeStage demande) {
    if (demande.getEmail() != null && !demande.getEmail().isBlank()) {
        return demande.getEmail().trim().toLowerCase(Locale.ROOT);
    }
    String commentaire = demande.getCommentaire();
        if (commentaire != null) {
            String marqueur = "Email candidat : ";
            int index = commentaire.indexOf(marqueur);
            if (index >= 0) {
                String email = commentaire.substring(index + marqueur.length()).trim();
                int finLigne = email.indexOf('\n');
                return finLigne >= 0 ? email.substring(0, finLigne).trim() : email;
            }
        }
    return "";
}

    private String genererMatriculeStagiaire() {
        int numero = (int) stagiaireRepository.count() + 1;
        String matricule;
        do {
            matricule = "STG-" + LocalDate.now().getYear() + "-" + String.format("%03d", numero++);
        } while (stagiaireRepository.findByMatricule(matricule).isPresent());
        return matricule;
    }

    private String valeurCsv(Object valeur) {
        if (valeur == null) return "\"\"";
        return "\"" + valeur.toString().replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }
}
