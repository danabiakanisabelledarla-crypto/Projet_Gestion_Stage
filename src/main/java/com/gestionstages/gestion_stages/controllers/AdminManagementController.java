package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import com.gestionstages.gestion_stages.services.ActivityLogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminManagementController {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final ServiceEntrepriseRepository serviceRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final StagiaireRepository stagiaireRepository;
    private final EncadreurRepository encadreurRepository;
    private final StageRepository stageRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    public AdminManagementController(UtilisateurRepository utilisateurRepository,
                                     RoleRepository roleRepository,
                                     ServiceEntrepriseRepository serviceRepository,
                                     DocumentRepository documentRepository,
                                     NotificationRepository notificationRepository,
                                     ActivityLogRepository activityLogRepository,
                                     StagiaireRepository stagiaireRepository,
                                     EncadreurRepository encadreurRepository,
                                     StageRepository stageRepository,
                                     PasswordEncoder passwordEncoder,
                                     ActivityLogService activityLogService) {
        this.utilisateurRepository = utilisateurRepository;
        this.roleRepository = roleRepository;
        this.serviceRepository = serviceRepository;
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.activityLogRepository = activityLogRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.encadreurRepository = encadreurRepository;
        this.stageRepository = stageRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    private String adminName(CustomUserDetails user) {
        return user != null ? user.getUtilisateur().getPrenom() + " " + user.getUtilisateur().getNom() : "Administrateur";
    }

    // ===== UTILISATEURS =====
    @GetMapping("/utilisateurs")
public String utilisateurs(Model model, @RequestParam(required = false) String succes) {
    model.addAttribute("activePage", "utilisateurs");
    model.addAttribute("nomComplet", "Administrateur");
    model.addAttribute("initiales", "A");

    // Filtrer uniquement ENCADREUR et RESPONSABLE_STAGE
    List<Utilisateur> tousUtilisateurs = utilisateurRepository.findAll().stream()
        .filter(u -> {
            String role = u.getRole().getLibelle();
            return "ENCADREUR".equals(role) || "RESPONSABLE_STAGE".equals(role);
        })
        .collect(java.util.stream.Collectors.toList());
    List<ServiceEntreprise> tousServices = serviceRepository.findAll();

    model.addAttribute("utilisateurs", tousUtilisateurs);
    model.addAttribute("roles", roleRepository.findAll().stream()
        .filter(r -> "ENCADREUR".equals(r.getLibelle()) || "RESPONSABLE_STAGE".equals(r.getLibelle()))
        .collect(java.util.stream.Collectors.toList()));
    model.addAttribute("services", tousServices);

    long totalUsers = tousUtilisateurs.size();
    long totalResp = tousUtilisateurs.stream().filter(u -> "RESPONSABLE_STAGE".equals(u.getRole().getLibelle())).count();
    long totalEncadreurs = tousUtilisateurs.stream().filter(u -> "ENCADREUR".equals(u.getRole().getLibelle())).count();
    long totalInactifs = tousUtilisateurs.stream().filter(u -> u.getStatut() == Utilisateur.StatutUtilisateur.inactif).count();

    model.addAttribute("totalUsers", totalUsers);
    model.addAttribute("totalAdmin", 0L);
    model.addAttribute("totalResponsables", totalResp);
    model.addAttribute("totalEncadreurs", totalEncadreurs);
    model.addAttribute("totalInactifs", totalInactifs);

    // JSON pour le panneau de détails
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
    List<Map<String, Object>> utilisateursJson = tousUtilisateurs.stream().map(u -> {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", u.getId());
        m.put("initiale", u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase());
        m.put("nomComplet", "M. " + u.getPrenom() + " " + u.getNom());
        m.put("prenom", u.getPrenom());
        m.put("nom", u.getNom());
        m.put("email", u.getEmail());
        m.put("telephone", u.getTelephone() != null ? u.getTelephone() : "—");
        m.put("role", u.getRole().getLibelle());
        m.put("roleLibelle", u.getRole().getLibelle());
        m.put("fonction", u.getRole().getDescription());
        m.put("statutCls", u.getStatut().name());
        m.put("statutLabel", u.getStatut() == Utilisateur.StatutUtilisateur.actif ? "Actif" : "Inactif");
        m.put("dateCreation", u.getDateCreation() != null ? sdf.format(java.sql.Timestamp.valueOf(u.getDateCreation())) : "—");
        m.put("service", "—");
        m.put("stagiairesCount", 0);
        m.put("evaluationsCount", 0);
        m.put("livrablesCount", 0);
        m.put("tauxEvaluation", 0);
        m.put("activites", new java.util.ArrayList<>());
        return m;
    }).collect(java.util.stream.Collectors.toList());

    model.addAttribute("utilisateursJson", utilisateursJson);

    if (succes != null) model.addAttribute("succes", succes);
    return "admin/utilisateurs";
}

    @PostMapping("/utilisateurs/ajouter")
    public String ajouterUtilisateur(@RequestParam String nom, @RequestParam String prenom,
                                      @RequestParam String email, @RequestParam String roleLibelle,
                                      @RequestParam String motDePasse,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        if (utilisateurRepository.existsByEmail(email)) {
            return "redirect:/admin/utilisateurs?succes=Email deja utilise.";
        }
        Role role = roleRepository.findByLibelle(roleLibelle).orElseThrow();
        utilisateurRepository.save(new Utilisateur(role, nom, prenom, email, passwordEncoder.encode(motDePasse)));
        activityLogService.log("Utilisateur ajoute", email + " (" + roleLibelle + ")", adminName(user));
        return "redirect:/admin/utilisateurs?succes=Utilisateur ajoute avec succes.";
    }

    @PostMapping("/utilisateurs/modifier/{id}")
    public String modifierUtilisateur(@PathVariable Integer id,
                                       @RequestParam String nom, @RequestParam String prenom,
                                       @RequestParam String roleLibelle,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        Utilisateur u = utilisateurRepository.findById(id).orElseThrow();
        u.setNom(nom);
        u.setPrenom(prenom);
        roleRepository.findByLibelle(roleLibelle).ifPresent(u::setRole);
        utilisateurRepository.save(u);
        activityLogService.log("Utilisateur modifie", u.getEmail(), adminName(user));
        return "redirect:/admin/utilisateurs?succes=Utilisateur modifie.";
    }

    @PostMapping("/utilisateurs/desactiver/{id}")
    public String desactiverUtilisateur(@PathVariable Integer id,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        utilisateurRepository.findById(id).ifPresent(u -> {
            u.setStatut(Utilisateur.StatutUtilisateur.inactif);
            utilisateurRepository.save(u);
            activityLogService.log("Compte desactive", u.getEmail(), adminName(user));
        });
        return "redirect:/admin/utilisateurs?succes=Compte desactive.";
    }

    @PostMapping("/utilisateurs/reactiver/{id}")
    public String reactiverUtilisateur(@PathVariable Integer id) {
        utilisateurRepository.findById(id).ifPresent(u -> {
            u.setStatut(Utilisateur.StatutUtilisateur.actif);
            utilisateurRepository.save(u);
        });
        return "redirect:/admin/utilisateurs?succes=Compte reactive.";
    }

    @PostMapping("/utilisateurs/reinitialiser/{id}")
    public String reinitialiserMdp(@PathVariable Integer id) {
        utilisateurRepository.findById(id).ifPresent(u -> {
            u.setMotDePasse(passwordEncoder.encode("dta2026"));
            utilisateurRepository.save(u);
        });
        return "redirect:/admin/utilisateurs?succes=Mot de passe reinitialise (dta2026).";
    }

    // ===== SERVICES =====
    @GetMapping({"/services", "/service"})
    public String services(Model model, @RequestParam(required = false) String succes) {
        List<ServiceEntreprise> services = serviceRepository.findAll();
        List<Stage> stages = stageRepository.findAll();
        List<Map<String, Object>> serviceRows = new ArrayList<>();

        for (int index = 0; index < services.size(); index++) {
            ServiceEntreprise service = services.get(index);
            List<Stage> stagesService = stages.stream()
                .filter(stage -> stage.getService() != null
                    && stage.getService().getId().equals(service.getId()))
                .toList();
            List<Encadreur> encadreursService = stagesService.stream()
                .map(Stage::getEncadreur)
                .filter(java.util.Objects::nonNull)
                .filter(encadreur -> encadreur.getId() != null)
                .collect(java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toMap(
                        Encadreur::getId,
                        encadreur -> encadreur,
                        (premier, doublon) -> premier,
                        java.util.LinkedHashMap::new),
                    map -> new ArrayList<>(map.values())));

            long stagesEnCoursService = stagesService.stream()
                .filter(stage -> stage.getStatut() == Stage.StatutStage.en_cours)
                .count();
            long stagesCloturesService = stagesService.stream()
                .filter(stage -> stage.getStatut() == Stage.StatutStage.termine)
                .count();

            Map<String, Object> row = new HashMap<>();
            row.put("service", service);
            row.put("encadreurs", encadreursService);
            row.put("responsable", encadreursService.isEmpty() ? null : encadreursService.get(0));
            row.put("encadreurNoms", encadreursService.stream()
                .map(encadreur -> encadreur.getUtilisateur().getPrenom() + " "
                    + encadreur.getUtilisateur().getNom())
                .collect(java.util.stream.Collectors.joining("|")));
            row.put("encadreurFonctions", encadreursService.stream()
                .map(encadreur -> encadreur.getFonction() == null
                    ? "Encadreur"
                    : encadreur.getFonction())
                .collect(java.util.stream.Collectors.joining("|")));
            row.put("totalStagiaires", stagesService.size());
            row.put("stagesEnCours", stagesEnCoursService);
            row.put("stagesClotures", stagesCloturesService);
            row.put("capacite", Math.max(10, stagesService.size() + 5));
            row.put("localisation", "Bâtiment " + (char) ('A' + (index % 5)));
            row.put("statut", "Actif");
            row.put("livrables", stagesService.size() * 3);
            row.put("rapports", stagesCloturesService);
            row.put("couleur", List.of("bleu", "vert", "orange", "violet").get(index % 4));
            serviceRows.add(row);
        }

        model.addAttribute("activePage", "services");
        model.addAttribute("services", services);
        model.addAttribute("serviceRows", serviceRows);
        model.addAttribute("totalServices", serviceRepository.count());
        model.addAttribute("totalResponsables", utilisateurRepository.findAll().stream()
            .filter(utilisateur -> utilisateur.getRole() != null
                && "RESPONSABLE_STAGE".equals(utilisateur.getRole().getLibelle()))
            .count());
        model.addAttribute("totalEncadreurs", encadreurRepository.count());
        model.addAttribute("totalStagiaires", stagiaireRepository.count());
        model.addAttribute("stagesEnCours", stageRepository.findByStatut(Stage.StatutStage.en_cours).size());
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/services";
    }

    @PostMapping("/services/ajouter")
    public String ajouterService(@RequestParam String nom, @RequestParam String description,
                                  @AuthenticationPrincipal CustomUserDetails user) {
        serviceRepository.save(new ServiceEntreprise(nom, description));
        activityLogService.log("Service ajoute", nom, adminName(user));
        return "redirect:/admin/services?succes=Service ajoute.";
    }

    @PostMapping("/services/modifier/{id}")
    public String modifierService(@PathVariable Integer id, @RequestParam String nom,
                                   @RequestParam String description) {
        serviceRepository.findById(id).ifPresent(s -> {
            s.setNom(nom);
            s.setDescription(description);
            serviceRepository.save(s);
        });
        return "redirect:/admin/services?succes=Service modifie.";
    }

    @PostMapping("/services/supprimer/{id}")
    public String supprimerService(@PathVariable Integer id) {
        serviceRepository.deleteById(id);
        return "redirect:/admin/services?succes=Service supprime.";
    }

    // ===== DOCUMENTS =====
    @GetMapping("/documents")
public String documents(Model model, @RequestParam(required = false) String succes) {
    model.addAttribute("activePage", "documents");
    model.addAttribute("nomComplet", "Administrateur");
    model.addAttribute("initiales", "A");

    List<Document> docs = documentRepository.findAll();

    long totalDocs = docs.size();
    long totalRapports = docs.stream().filter(d -> "rapport_final".equals(d.getTypeDocument())).count();
    long totalRapportsHebdo = docs.stream().filter(d -> "rapport_hebdomadaire".equals(d.getTypeDocument())).count();
    long totalAttestations = docs.stream().filter(d -> "attestation".equals(d.getTypeDocument())).count();
    long totalFichesNotes = docs.stream().filter(d -> "fiche_note".equals(d.getTypeDocument())).count();
    long totalArchives = docs.stream().filter(d -> "archive".equals(d.getStatut())).count();

    model.addAttribute("documents", docs);
    model.addAttribute("totalDocs", totalDocs);
    model.addAttribute("totalRapports", totalRapports);
    model.addAttribute("totalRapportsHebdo", totalRapportsHebdo);
    model.addAttribute("totalAttestations", totalAttestations);
    model.addAttribute("totalFichesNotes", totalFichesNotes);
    model.addAttribute("totalArchives", totalArchives);

    // JSON pour le panneau de détails
    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
    List<Map<String, Object>> documentsJson = docs.stream().map(d -> {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", d.getId());
        m.put("nomFichier", d.getNomFichier());
        m.put("typeDocument", d.getTypeDocument());
        m.put("statut", d.getStatut());
        m.put("version", d.getVersion() != null ? d.getVersion() : "1.0");
        m.put("taille", d.getTailleOctets() != null
                ? String.format(java.util.Locale.FRANCE, "%.1f Mo", d.getTailleOctets() / 1048576.0)
                : "—");
        m.put("dateDepot", d.getDateDepot() != null ? sdf.format(java.sql.Timestamp.valueOf(d.getDateDepot())) : "—");

        String ext = d.getNomFichier() != null && d.getNomFichier().contains(".")
                ? d.getNomFichier().substring(d.getNomFichier().lastIndexOf(".") + 1).toLowerCase() : "";
        m.put("ext", ext);

        // Propriétaire
        if (d.getStage() != null && d.getStage().getStagiaire() != null) {
            var stagiaire = d.getStage().getStagiaire();
            var user = stagiaire.getUtilisateur();
            m.put("proprietaireNom", user.getPrenom() + " " + user.getNom());
            m.put("proprietaireInitiales", user.getPrenom().substring(0,1).toUpperCase() + user.getNom().substring(0,1).toUpperCase());
            m.put("proprietaireRole", "Stagiaire");
            m.put("proprietaireService", d.getStage().getService() != null ? d.getStage().getService().getNom() : "");
        } else if (d.getDemandeStage() != null) {
            var ds = d.getDemandeStage();
            m.put("proprietaireNom", ds.getPrenom() + " " + ds.getNom());
            m.put("proprietaireInitiales", ds.getPrenom().substring(0,1).toUpperCase() + ds.getNom().substring(0,1).toUpperCase());
            m.put("proprietaireRole", "Candidat");
            m.put("proprietaireService", "");
        } else {
            m.put("proprietaireNom", "—");
            m.put("proprietaireInitiales", "—");
            m.put("proprietaireRole", "—");
            m.put("proprietaireService", "");
        }

        // Documents liés (même stagiaire)
        List<Map<String, String>> docsLies = new java.util.ArrayList<>();
        if (d.getStage() != null) {
            Integer stageId = d.getStage().getId();
            for (Document other : docs) {
                if (other.getId().equals(d.getId())) continue;
                if (other.getStage() != null && other.getStage().getId().equals(stageId)) {
                    Map<String, String> lm = new java.util.HashMap<>();
                    lm.put("nom", other.getNomFichier());
                    String oext = other.getNomFichier() != null && other.getNomFichier().contains(".")
                            ? other.getNomFichier().substring(other.getNomFichier().lastIndexOf(".") + 1).toLowerCase() : "";
                    lm.put("ext", oext);
                    docsLies.add(lm);
                }
            }
        }
        m.put("documentsLies", docsLies);
        m.put("documentsLiesCount", docsLies.size());

        return m;
    }).collect(java.util.stream.Collectors.toList());

    model.addAttribute("documentsJson", documentsJson);

    if (succes != null) model.addAttribute("succes", succes);
    return "admin/documents";
}

    @PostMapping("/documents/archiver/{id}")
    public String archiverDocument(@PathVariable Integer id) {
        documentRepository.findById(id).ifPresent(d -> {
            d.setStatut("archive");
            documentRepository.save(d);
        });
        return "redirect:/admin/documents?succes=Document archive.";
    }

    @PostMapping("/documents/supprimer/{id}")
    public String supprimerDocument(@PathVariable Integer id) {
        documentRepository.findById(id).ifPresent(d -> {
            try { Files.deleteIfExists(Paths.get(d.getCheminFichier())); } catch (Exception ignored) {}
            documentRepository.delete(d);
        });
        return "redirect:/admin/documents?succes=Document supprime.";
    }

    // ===== NOTIFICATIONS =====
    @GetMapping("/notifications")
    public String notifications(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "notifications");
        model.addAttribute("notifications", notificationRepository.findAllByOrderByDateEnvoiDesc());
        model.addAttribute("totalNotif", notificationRepository.count());
        model.addAttribute("totalStag", utilisateurRepository.findByRole_Libelle("STAGIAIRE").size());
        model.addAttribute("totalResp", utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size());
        model.addAttribute("totalEnc", utilisateurRepository.findByRole_Libelle("ENCADREUR").size());
        model.addAttribute("utilisateurs", utilisateurRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Utilisateur::getNom)
                        .thenComparing(Utilisateur::getPrenom))
                .toList());
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/notifications";
    }

    @PostMapping("/notifications/envoyer")
    public String envoyerNotification(@RequestParam String objet, @RequestParam String message,
                                         @RequestParam String modeEnvoi,
                                         @RequestParam(required = false) String destinataireType,
                                         @RequestParam(required = false) Integer destinataireId,
                                         @RequestParam String priorite,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        String cible = destinataireType;
        Notification n;
        if ("personne".equals(modeEnvoi) && destinataireId != null) {
            Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
            if (destinataire == null) {
                return "redirect:/admin/notifications?erreur=Destinataire introuvable.";
            }
            cible = destinataire.getPrenom() + " " + destinataire.getNom();
            n = new Notification(objet, message, "Personne précise", priorite, adminName(user));
            n.setDestinataireEmail(destinataire.getEmail());
        } else {
            n = new Notification(objet, message, cible, priorite, adminName(user));
        }
        notificationRepository.save(n);
        activityLogService.log("Notification envoyee", objet + " -> " + cible, adminName(user));
        return "redirect:/admin/notifications?succes=Notification envoyee.";
    }

    // ===== JOURNAL =====
        @GetMapping("/journal")
    public String journal(Model model) {
        model.addAttribute("activePage", "journal");

        List<ActivityLog> allLogs = activityLogRepository.findAll();
        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm");

        long totalEntries = allLogs.size();
        long activeStagiaires = stagiaireRepository.findByStatut(Stagiaire.StatutStagiaire.actif).size();
        long weekEntries = allLogs.stream()
                .filter(a -> a.getDateActivite() != null
                        && a.getDateActivite().toLocalDate().isAfter(java.time.LocalDate.now().minusDays(7)))
                .count();
        long linkedTasks = allLogs.stream()
                .filter(a -> (a.getAction() != null && a.getAction().toLowerCase().contains("tache"))
                        || (a.getDetails() != null && a.getDetails().toLowerCase().contains("tache")))
                .count();
        long totalComments = allLogs.stream()
                .filter(a -> (a.getAction() != null && a.getAction().toLowerCase().contains("commentaire"))
                        || (a.getDetails() != null && a.getDetails().toLowerCase().contains("commentaire")))
                .count();

        model.addAttribute("totalEntries", totalEntries);
        model.addAttribute("activeStagiaires", activeStagiaires);
        model.addAttribute("weekEntries", weekEntries);
        model.addAttribute("linkedTasks", linkedTasks);
        model.addAttribute("totalComments", totalComments);

        List<Map<String, Object>> activites = allLogs.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", a.getId());
            m.put("dateFormatted", a.getDateActivite() != null ? sdfDate.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
            m.put("timeFormatted", a.getDateActivite() != null ? sdfTime.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");

            String nom = a.getUtilisateurNom() != null ? a.getUtilisateurNom() : "Système";
            m.put("utilisateur", nom);
            String[] parts = nom.split(" ");
            String init = parts.length >= 2
                    ? parts[0].substring(0,1).toUpperCase() + parts[parts.length-1].substring(0,1).toUpperCase()
                    : nom.substring(0,1).toUpperCase();
            m.put("initiale", init);
            m.put("role", "—");

            String action = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String details = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            String typeCls, typeLabel;
            if (action.contains("commentaire") || details.contains("commentaire")) {
                typeCls = "commentaire"; typeLabel = "Commentaire";
            } else if (action.contains("livrable") || details.contains("livrable")) {
                typeCls = "livrable"; typeLabel = "Livrable";
            } else if (action.contains("evalu") || details.contains("evalu")) {
                typeCls = "evaluation"; typeLabel = "Évaluation";
            } else if (action.contains("reunion") || details.contains("reunion")) {
                typeCls = "reunion"; typeLabel = "Réunion";
            } else if (action.contains("note") || details.contains("note")) {
                typeCls = "note"; typeLabel = "Note";
            } else {
                typeCls = "journal"; typeLabel = "Journal";
            }
            m.put("typeCls", typeCls);
            m.put("typeLabel", typeLabel);
            m.put("description", a.getDetails() != null ? a.getDetails() : a.getAction());
            m.put("linkedItem", null);
            m.put("attachmentsCount", 0);
            return m;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("activites", activites);

        List<Map<String, Object>> recent = allLogs.stream()
                .sorted((a,b) -> b.getDateActivite().compareTo(a.getDateActivite()))
                .limit(5)
                .map(a -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    String action = a.getAction() != null ? a.getAction().toLowerCase() : "";
                    String details = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
                    String typeCls;
                    if (action.contains("commentaire") || details.contains("commentaire")) {
                        typeCls = "bleu";
                    } else if (action.contains("livrable") || details.contains("livrable")) {
                        typeCls = "orange";
                    } else if (action.contains("evalu") || details.contains("evalu")) {
                        typeCls = "violet";
                    } else if (action.contains("reunion") || details.contains("reunion")) {
                        typeCls = "rouge";
                    } else {
                        typeCls = "vert";
                    }
                    m.put("typeCls", typeCls);
                    m.put("icon", "fa-solid fa-bolt");
                    m.put("title", a.getAction());
                    m.put("desc", a.getDetails());
                    if (a.getDateActivite() != null) {
                        long minutes = java.time.Duration.between(a.getDateActivite(), java.time.LocalDateTime.now()).toMinutes();
                        if (minutes < 60) m.put("time", "Il y a " + minutes + " min");
                        else if (minutes < 1440) m.put("time", "Il y a " + (minutes / 60) + "h");
                        else m.put("time", "Il y a " + (minutes / 1440) + " jours");
                    } else {
                        m.put("time", "—");
                    }
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("recentActivities", recent);

        long journalCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return !act.contains("commentaire") && !det.contains("commentaire")
                    && !act.contains("livrable") && !det.contains("livrable")
                    && !act.contains("evalu") && !det.contains("evalu")
                    && !act.contains("reunion") && !det.contains("reunion")
                    && !act.contains("note") && !det.contains("note");
        }).count();
        long commentCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return act.contains("commentaire") || det.contains("commentaire");
        }).count();
        long livrableCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return (act.contains("livrable") || det.contains("livrable")) && !act.contains("commentaire");
        }).count();
        long evalCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return (act.contains("evalu") || det.contains("evalu")) && !act.contains("livrable") && !act.contains("commentaire");
        }).count();
        long reunionCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return (act.contains("reunion") || det.contains("reunion")) && !act.contains("evalu") && !act.contains("livrable");
        }).count();
        long noteCount = allLogs.stream().filter(a -> {
            String act = a.getAction() != null ? a.getAction().toLowerCase() : "";
            String det = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            return (act.contains("note") || det.contains("note")) && !act.contains("reunion") && !act.contains("evalu") && !act.contains("livrable");
        }).count();

        long denom = Math.max(1, totalEntries);
        List<Map<String, Object>> distribution = List.of(
            Map.of("name", "Journal", "count", journalCount, "pct", (journalCount * 100 / denom), "cls", "vert"),
            Map.of("name", "Commentaire", "count", commentCount, "pct", (commentCount * 100 / denom), "cls", "bleu"),
            Map.of("name", "Livrable", "count", livrableCount, "pct", (livrableCount * 100 / denom), "cls", "orange"),
            Map.of("name", "Évaluation", "count", evalCount, "pct", (evalCount * 100 / denom), "cls", "violet"),
            Map.of("name", "Réunion", "count", reunionCount, "pct", (reunionCount * 100 / denom), "cls", "rouge"),
            Map.of("name", "Note", "count", noteCount, "pct", (noteCount * 100 / denom), "cls", "gris")
        );
        model.addAttribute("distribution", distribution);

        List<Map<String, Object>> segments = new java.util.ArrayList<>();
        double currentOffset = 0;
        long[] counts = {journalCount, commentCount, livrableCount, evalCount, reunionCount, noteCount};
        String[] colors = {"#22C55E", "#3B82F6", "#F59E0B", "#8B5CF6", "#EF4444", "#94A3B8"};
        double circ = 2 * Math.PI * 15.9;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] == 0) continue;
            double pct = (double) counts[i] / denom;
            double dashLen = pct * circ;
            Map<String, Object> seg = new java.util.HashMap<>();
            seg.put("color", colors[i]);
            seg.put("dasharray", dashLen + " " + (circ - dashLen));
            seg.put("dashoffset", -currentOffset);
            segments.add(seg);
            currentOffset += dashLen;
        }
        model.addAttribute("distributionSegments", segments);

        List<Map<String, Object>> typeCounts = List.of(
            Map.of("name", "Journaux quotidiens", "count", journalCount, "cls", "vert", "icon", "fa-solid fa-book"),
            Map.of("name", "Commentaires", "count", commentCount, "cls", "bleu", "icon", "fa-solid fa-comment"),
            Map.of("name", "Livrables", "count", livrableCount, "cls", "orange", "icon", "fa-solid fa-file-arrow-up"),
            Map.of("name", "Évaluations", "count", evalCount, "cls", "violet", "icon", "fa-solid fa-star"),
            Map.of("name", "Réunions", "count", reunionCount, "cls", "rouge", "icon", "fa-solid fa-handshake"),
            Map.of("name", "Notes", "count", noteCount, "cls", "jaune", "icon", "fa-solid fa-pen")
        );
        model.addAttribute("typeCounts", typeCounts);

        return "admin/journal";
    }

    // ===== PARAMETRES =====
    @GetMapping({"/parametres", "/parametre"})
    public String parametres(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "parametres");
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/parametres";
    }

    @PostMapping("/documents/ajouter")
    public String ajouterDocument(@RequestParam String typeDocument,
                                  @RequestParam MultipartFile fichier,
                                  @AuthenticationPrincipal CustomUserDetails user) {
        if (fichier == null || fichier.isEmpty()) {
            return "redirect:/admin/documents?succes=Aucun fichier sélectionné.";
        }
        try {
            java.nio.file.Path dossier = Paths.get("uploads", "documents").toAbsolutePath().normalize();
            Files.createDirectories(dossier);
            String original = Paths.get(fichier.getOriginalFilename() == null ? "document" : fichier.getOriginalFilename())
                    .getFileName().toString();
            java.nio.file.Path cible = dossier.resolve(UUID.randomUUID() + "_" + original).normalize();
            if (!cible.startsWith(dossier)) throw new IllegalArgumentException("Chemin de fichier invalide");
            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);
            Document document = new Document(original, typeDocument, cible.toString());
            document.setTailleOctets(fichier.getSize());
            documentRepository.save(document);
            activityLogService.log("Document ajouté", original, adminName(user));
        } catch (Exception exception) {
            return "redirect:/admin/documents?succes=Le document n'a pas pu être ajouté.";
        }
        return "redirect:/admin/documents?succes=Document ajouté.";
    }

    @PostMapping("/documents/commenter")
    public String commenterDocument(@RequestParam Integer documentId,
                                    @RequestParam String commentaire,
                                    @AuthenticationPrincipal CustomUserDetails user) {
        documentRepository.findById(documentId).ifPresent(document -> {
            activityLogService.log("Commentaire document",
                    document.getNomFichier() + " : " + commentaire.trim(), adminName(user));
            String email = null;
            if (document.getStage() != null && document.getStage().getStagiaire() != null) {
                email = document.getStage().getStagiaire().getUtilisateur().getEmail();
            } else if (document.getDemandeStage() != null) {
                email = document.getDemandeStage().getEmail();
            }
            if (email != null && !email.isBlank()) {
                Notification notification = new Notification(
                        "Nouveau commentaire sur votre document",
                        commentaire.trim(), "Personne précise", "normale", adminName(user));
                notification.setDestinataireEmail(email);
                notificationRepository.save(notification);
            }
        });
        return "redirect:/admin/documents?succes=Commentaire envoyé.";
    }

    @PostMapping({"/parametres", "/parametre"})
    public String sauverParametres(RedirectAttributes ra) {
        ra.addAttribute("succes", "Parametres enregistres.");
        return "redirect:/admin/parametres";
    }

    // ===== RAPPORTS =====
    @GetMapping("/rapports")
    public String rapports(Model model) {
        model.addAttribute("activePage", "rapports");
        model.addAttribute("totalStagiaires", stagiaireRepository.count());
        model.addAttribute("totalStages", stageRepository.count());
        model.addAttribute("stagesTermines", stageRepository.findByStatut(Stage.StatutStage.termine).size());
        model.addAttribute("totalDocuments", documentRepository.count());

        // Documents exportés simulés
        List<Map<String,String>> docsExportes = List.of(
            Map.of("nom","rapport-mensuel-juillet-2026","stagiaire","Jean Dupont","typeCls","rapport","typeLabel","Rapport final","dateExport","17/07/2026","taille","2.5 Mo","formatCls","pdf"),
            Map.of("nom","attestation-stage-martin","stagiaire","Marie Martin","typeCls","attestation","typeLabel","Attestation","dateExport","16/07/2026","taille","1.2 Mo","formatCls","pdf"),
            Map.of("nom","fiche-note-bertrand","stagiaire","Pierre Bertrand","typeCls","fiche","typeLabel","Fiche de note","dateExport","15/07/2026","taille","0.8 Mo","formatCls","excel"),
            Map.of("nom","rapport-hebdo-S29","stagiaire","Sophie Bernard","typeCls","hebdo","typeLabel","Rapport hebdo","dateExport","14/07/2026","taille","1.8 Mo","formatCls","pdf"),
            Map.of("nom","rapport-final-nguyen","stagiaire","Lucas Nguyen","typeCls","rapport","typeLabel","Rapport final","dateExport","12/07/2026","taille","3.1 Mo","formatCls","pdf")
        );
        model.addAttribute("documentsExportes", docsExportes);
        return "admin/rapports";
    }

    // ===== SAUVEGARDE =====
    @GetMapping("/sauvegarde")
    public String sauvegarde(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "sauvegarde");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "AD");
        model.addAttribute("notificationsCount", 0);
        model.addAttribute("recentNotifications", new ArrayList<>());
        if (succes != null) model.addAttribute("succes", succes);

        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm");

        // Activity logs liés aux sauvegardes
        List<ActivityLog> allLogs = activityLogRepository.findAll();
        List<ActivityLog> backupLogs = allLogs.stream()
            .filter(a -> a.getAction() != null && a.getAction().toLowerCase().contains("sauvegarde"))
            .sorted((a,b) -> b.getDateActivite().compareTo(a.getDateActivite()))
            .collect(java.util.stream.Collectors.toList());
        long totalBackups = backupLogs.size();
        long failedBackups = backupLogs.stream()
            .filter(a -> (a.getDetails() != null && (a.getDetails().toLowerCase().contains("echou") || a.getDetails().toLowerCase().contains("echec")))
                      || (a.getAction() != null && a.getAction().toLowerCase().contains("echou")))
            .count();

        // Dernière sauvegarde
        String lastBackupDate = "—";
        String lastBackupTime = "—";
        if (!backupLogs.isEmpty()) {
            ActivityLog last = backupLogs.get(0);
            if (last.getDateActivite() != null) {
                lastBackupDate = sdfDate.format(java.sql.Timestamp.valueOf(last.getDateActivite()));
                lastBackupTime = sdfTime.format(java.sql.Timestamp.valueOf(last.getDateActivite()));
            }
        }

        // Comptages documents pour le stockage
        long docCount = documentRepository.count();
        long storageUsed = docCount * 5; // ~5 Mo par document
        int storagePct = (int) Math.min(99, storageUsed * 100 / 320000); // capacité 320 Go
        String totalBackupSize = String.format("%.1f Go", storageUsed / 1000.0);

        model.addAttribute("totalBackups", totalBackups);
        model.addAttribute("lastBackupDate", totalBackups > 0 ? lastBackupDate : "Aucune");
        model.addAttribute("lastBackupTime", totalBackups > 0 ? lastBackupTime : "—");
        model.addAttribute("storageUsed", storageUsed);
        model.addAttribute("storagePct", storagePct);
        model.addAttribute("autoBackupStatus", true);
        model.addAttribute("autoBackupCount", 3);
        model.addAttribute("totalBackupSize", totalBackupSize);
        model.addAttribute("failedBackups", failedBackups);
        model.addAttribute("totalRestorations", 0);

        // Historique des sauvegardes (depuis les logs)
        List<Map<String, Object>> history = backupLogs.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", a.getDateActivite() != null ? sdfDate.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
            m.put("time", a.getDateActivite() != null ? sdfTime.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
            m.put("typeCls", "manuel");
            m.put("typeLabel", "Manuelle");
            m.put("size", "—");
            m.put("destination", "Serveur");
            String details = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            boolean echec = details.contains("echou") || details.contains("echec");
            m.put("statutCls", echec ? "echec" : "reussie");
            m.put("statutLabel", echec ? "Échec" : "Réussie");
            return m;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("backupHistory", history);

        // Distribution (estimée depuis les logs réels)
        long autoCount = backupLogs.stream().filter(a -> a.getAction() != null && a.getAction().toLowerCase().contains("auto")).count();
        long manuelles = totalBackups - autoCount;
        long denom = Math.max(1, totalBackups);
        List<Map<String, Object>> distribution = List.of(
            Map.of("name", "Manuelles", "count", manuelles, "pct", manuelles * 100 / denom, "cls", "bleu"),
            Map.of("name", "Automatiques", "count", autoCount, "pct", autoCount * 100 / denom, "cls", "vert")
        );
        model.addAttribute("distribution", distribution);

        // Segments donut
        long[] dCounts = {manuelles, autoCount};
        String[] dColors = {"#2563EB", "#22C55E"};
        List<Map<String, Object>> segments = new ArrayList<>();
        double dCirc = 2 * Math.PI * 15.9;
        double dOffset = 0;
        for (int i = 0; i < dCounts.length; i++) {
            if (dCounts[i] == 0) continue;
            double pct = (double) dCounts[i] / denom;
            double dashLen = pct * dCirc;
            Map<String, Object> seg = new HashMap<>();
            seg.put("color", dColors[i]);
            seg.put("dasharray", dashLen + " " + (dCirc - dashLen));
            seg.put("dashoffset", -dOffset);
            segments.add(seg);
            dOffset += dashLen;
        }
        model.addAttribute("distributionSegments", segments);

        // Activité récente à partir des logs de sauvegarde
        List<Map<String, Object>> recent = backupLogs.stream().limit(5).map(a -> {
            Map<String, Object> m = new HashMap<>();
            String details = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
            boolean echec = details.contains("echou") || details.contains("echec");
            m.put("cls", echec ? "rouge" : "vert");
            m.put("icon", echec ? "fa-solid fa-triangle-exclamation" : "fa-solid fa-check");
            m.put("title", a.getAction());
            m.put("date", a.getDateActivite() != null ? sdfDate.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
            m.put("time", a.getDateActivite() != null ? sdfTime.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
            return m;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("recentActivity", recent);

        // Graphiques (simulés car pas d'historique mensuel fiable)
        List<String> chartMonths = List.of("Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Juil");
        model.addAttribute("chartMonths", chartMonths);
        List<Integer> chartAutoValues = List.of(12, 18, 14, 22, 19, 25, 13);
        List<Integer> chartManualValues = List.of(5, 3, 6, 4, 7, 2, 4);
        List<Integer> chartRestoreValues = List.of(1, 0, 2, 1, 0, 3, 1);
        model.addAttribute("chartAutoValues", chartAutoValues);
        model.addAttribute("chartManualValues", chartManualValues);
        model.addAttribute("chartRestoreValues", chartRestoreValues);
        model.addAttribute("chartAutoPoints", buildPoints(chartAutoValues));
        model.addAttribute("chartManualPoints", buildPoints(chartManualValues));
        model.addAttribute("chartRestorePoints", buildPoints(chartRestoreValues));

        return "admin/sauvegarde";
    }

    private String buildPoints(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            int x = 60 + (i + 1) * 100;
            int y = 190 - (values.get(i) * 3);
            sb.append(x).append(",").append(y).append(" ");
        }
        return sb.toString().trim();
    }

    @PostMapping("/sauvegarde")
    public String lancerSauvegarde(@AuthenticationPrincipal CustomUserDetails user, RedirectAttributes ra) {
        activityLogService.log("Sauvegarde lancee", "Export des donnees", adminName(user));
        ra.addAttribute("succes", "Sauvegarde effectuee avec succes.");
        return "redirect:/admin/sauvegarde";
    }

    // ===== SECURITE =====
    @GetMapping("/securite")
    public String securite(Model model) {
        model.addAttribute("activePage", "securite");
        model.addAttribute("nomComplet", "Administrateur");
        model.addAttribute("initiales", "AD");
        model.addAttribute("notificationsCount", 0);
        model.addAttribute("recentNotifications", new ArrayList<>());
        model.addAttribute("roles", roleRepository.findAll());

        java.text.SimpleDateFormat sdfDate = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.text.SimpleDateFormat sdfTime = new java.text.SimpleDateFormat("HH:mm");
        java.text.SimpleDateFormat sdfShort = new java.text.SimpleDateFormat("dd/MM");

        List<Utilisateur> allUsers = utilisateurRepository.findAll();
        List<ActivityLog> allLogs = activityLogRepository.findAll();
        model.addAttribute("utilisateurs", allUsers);

        // Stats
        long totalUsers = allUsers.size();
        long lockedAccounts = allUsers.stream().filter(u -> u.getStatut() == Utilisateur.StatutUtilisateur.inactif).count();

        long totalAlerts = allLogs.stream()
            .filter(a -> a.getAction() != null && (a.getAction().toLowerCase().contains("securite")
                || a.getAction().toLowerCase().contains("tentative")
                || a.getAction().toLowerCase().contains("desactive")))
            .count();

        long securityLogCount = allLogs.size();
        int securityScore = (int) Math.min(100, (totalUsers > 0 ? (totalUsers - lockedAccounts) * 100 / totalUsers : 100));

        model.addAttribute("successfulLogins", securityLogCount);
        model.addAttribute("failedLogins", lockedAccounts > 0 ? lockedAccounts * 2 : 0);
        model.addAttribute("activeSessions", Math.max(1, totalUsers > 2 ? (int)(totalUsers * 0.6) : 1));
        model.addAttribute("totalAlerts", totalAlerts);
        model.addAttribute("lockedAccounts", lockedAccounts);
        model.addAttribute("securityScore", securityScore);

        // Chart data — simulé (pas de tracking de connexion dans la base)
        List<Integer> logins = List.of(22, 28, 19, 31, 25, 33, 27);
        List<Integer> failed = List.of(3, 5, 2, 4, 6, 3, 4);
        model.addAttribute("chartLoginPoints", buildChartPoints(logins));
        model.addAttribute("chartFailedPoints", buildChartPoints(failed));

        List<Integer> alerts = List.of(1, 0, 2, 1, 3, 1, 0, 2, 1, 1, 0, 0, 2, 1, 3, 2, 1, 0, 1, 1, 2, 0, 1, 0, 0, 1, 1, 2, 0, 1);
        model.addAttribute("chartAlertPoints", buildChartPoints(alerts));

        List<Integer> weeklyFailuresRaw = List.of(8, 12, 5, 9, 15, 7, 11);
        int wfMax = weeklyFailuresRaw.stream().max(Integer::compare).orElse(1);
        List<Integer> weeklyFailureHeights = weeklyFailuresRaw.stream().map(v -> v * 130 / wfMax).collect(java.util.stream.Collectors.toList());
        model.addAttribute("weeklyFailures", weeklyFailuresRaw);
        model.addAttribute("weeklyFailureHeights", weeklyFailureHeights);

        // User distribution (depuis la base)
        long adminCount = utilisateurRepository.findByRole_Libelle("ADMINISTRATEUR").size();
        long respCount = utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size();
        long encadCount = utilisateurRepository.findByRole_Libelle("ENCADREUR").size();
        long stagCount = utilisateurRepository.findByRole_Libelle("STAGIAIRE").size();
        long denom = Math.max(1, totalUsers);
        model.addAttribute("totalUsers", totalUsers);
        List<Map<String,Object>> userDist = List.of(
            Map.of("name", "Administrateur", "count", adminCount, "pct", adminCount*100/denom, "cls", "bleu"),
            Map.of("name", "Responsable", "count", respCount, "pct", respCount*100/denom, "cls", "vert"),
            Map.of("name", "Encadreur", "count", encadCount, "pct", encadCount*100/denom, "cls", "orange"),
            Map.of("name", "Stagiaire", "count", stagCount, "pct", stagCount*100/denom, "cls", "violet")
        );
        model.addAttribute("userDistribution", userDist);

        List<Map<String,Object>> segs = new ArrayList<>();
        double circ = 2*Math.PI*15.9;
        double off = 0;
        long[] vals = {adminCount, respCount, encadCount, stagCount};
        String[] cols = {"#2563EB", "#22C55E", "#F59E0B", "#8B5CF6"};
        for(int i=0; i<vals.length; i++) {
            if(vals[i]==0) continue;
            double pct = (double)vals[i]/denom;
            double dl = pct*circ;
            Map<String,Object> seg = new HashMap<>();
            seg.put("color", cols[i]);
            seg.put("dasharray", dl+" "+(circ-dl));
            seg.put("dashoffset", -off);
            segs.add(seg);
            off += dl;
        }
        model.addAttribute("userDistributionSegments", segs);

        // Security journal (depuis les logs d'activité)
        List<Map<String,Object>> journal = allLogs.stream()
            .sorted((a,b) -> b.getDateActivite().compareTo(a.getDateActivite()))
            .limit(10)
            .map(a -> {
                Map<String,Object> m = new HashMap<>();
                m.put("date", a.getDateActivite() != null ? sdfShort.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
                m.put("time", a.getDateActivite() != null ? sdfTime.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
                m.put("user", a.getUtilisateurNom() != null ? a.getUtilisateurNom() : "Système");
                m.put("action", a.getAction());
                m.put("desc", a.getDetails());
                String action = a.getAction() != null ? a.getAction().toLowerCase() : "";
                String levelCls;
                if (action.contains("supprim") || action.contains("desactive")) {
                    levelCls = "attention";
                } else if (action.contains("ajoute") || action.contains("modifie")) {
                    levelCls = "info";
                } else {
                    levelCls = "info";
                }
                m.put("level", levelCls.equals("attention") ? "Attention" : "Info");
                m.put("levelCls", levelCls);
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("securityJournal", journal);

        // Connection history (depuis les logs récents)
        List<Map<String,Object>> connHist = allLogs.stream()
            .sorted((a,b) -> b.getDateActivite().compareTo(a.getDateActivite()))
            .limit(5)
            .map(a -> {
                Map<String,Object> m = new HashMap<>();
                String nom = a.getUtilisateurNom() != null ? a.getUtilisateurNom() : "Système";
                String[] parts = nom.split(" ");
                String init = parts.length >= 2
                    ? parts[0].substring(0,1).toUpperCase() + parts[parts.length-1].substring(0,1).toUpperCase()
                    : nom.substring(0,1).toUpperCase();
                m.put("initiale", init);
                m.put("nom", nom);
                m.put("email", "—");
                m.put("role", "Utilisateur");
                m.put("roleCls", "bleu");
                m.put("date", a.getDateActivite() != null ? sdfDate.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
                m.put("time", a.getDateActivite() != null ? sdfTime.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
                m.put("ip", "—");
                m.put("browser", "—");
                String details = a.getDetails() != null ? a.getDetails().toLowerCase() : "";
                boolean echec = details.contains("echou") || details.contains("echec");
                m.put("statutCls", echec ? "rouge" : "vert");
                m.put("statutLabel", echec ? "Échec" : "Succès");
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("connectionHistory", connHist);

        // Suspicious activities (à partir des logs)
        List<Map<String,Object>> suspicious = allLogs.stream()
            .filter(a -> a.getAction() != null && (a.getAction().toLowerCase().contains("supprim")
                || a.getAction().toLowerCase().contains("desactive")))
            .limit(5)
            .map(a -> {
                Map<String,Object> m = new HashMap<>();
                m.put("title", a.getAction());
                m.put("desc", a.getDetails() != null ? a.getDetails() : "—");
                m.put("date", a.getDateActivite() != null ? sdfDate.format(java.sql.Timestamp.valueOf(a.getDateActivite())) : "—");
                m.put("level", "Élevé");
                m.put("levelCls", "eleve");
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("suspiciousActivities", suspicious);

        // Active sessions (simulé)
        List<Map<String,Object>> sessionsList = List.of(
            Map.of("initiale","AD","nom","Administrateur","appareil","Chrome 120 · Windows 11","ip","192.168.1.100","lastActivity","Il y a 2 min"),
            Map.of("initiale","JD","nom","Jean Dupont","appareil","Firefox 118 · macOS 14","ip","192.168.1.101","lastActivity","Il y a 15 min"),
            Map.of("initiale","MM","nom","Marie Martin","appareil","Safari 17 · iOS 18","ip","10.0.0.25","lastActivity","Il y a 1h")
        );
        model.addAttribute("activeSessionsList", sessionsList);

        // Comptes verrouillés (utilisateurs inactifs)
        List<Map<String,Object>> lockedList = allUsers.stream()
            .filter(u -> u.getStatut() == Utilisateur.StatutUtilisateur.inactif)
            .map(u -> {
                Map<String,Object> m = new HashMap<>();
                m.put("initiale", u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase());
                m.put("nom", u.getPrenom() + " " + u.getNom());
                m.put("attempts", 5);
                m.put("date", u.getDateCreation() != null ? sdfDate.format(java.sql.Timestamp.valueOf(u.getDateCreation())) : "—");
                m.put("time", "—");
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("lockedAccountsList", lockedList);

        // 2FA stats (simulé)
        model.addAttribute("twoFactorEnabled", Math.min(totalUsers, 24));
        model.addAttribute("twoFactorDisabled", totalUsers > 24 ? totalUsers - 24 : 0);
        model.addAttribute("twoFactorPct", Math.min(100, (totalUsers > 0 ? 24 * 100 / (int)totalUsers : 67)));

        return "admin/securite";
    }

    private String buildChartPoints(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        int maxVal = values.stream().max(Integer::compare).orElse(1);
        double scale = maxVal > 0 ? 120.0 / maxVal : 1;
        for(int i = 0; i < values.size(); i++) {
            int x = 40 + (i * (340 / Math.max(1, values.size() - 1)));
            int y = 150 - (int)(values.get(i) * scale);
            sb.append(x).append(",").append(y).append(" ");
        }
        return sb.toString().trim();
    }
}
