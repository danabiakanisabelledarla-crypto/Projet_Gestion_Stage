package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;
import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import com.gestionstages.gestion_stages.services.ActivityLogService;
import com.gestionstages.gestion_stages.services.ApplicationSettingService;
import com.gestionstages.gestion_stages.services.BackupService;
import com.gestionstages.gestion_stages.services.SecurityScanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

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
    private final EmailService emailService;
    private final AdminPreferenceRepository adminPreferenceRepository;
    private final SessionRegistry sessionRegistry;
    private final ApplicationSettingService applicationSettingService;
    private final JdbcTemplate jdbcTemplate;
    private final BackupService backupService;
    private final SecurityScanService securityScanService;

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
                                     ActivityLogService activityLogService,
                                     EmailService emailService,
                                     AdminPreferenceRepository adminPreferenceRepository,
                                     SessionRegistry sessionRegistry,
                                     ApplicationSettingService applicationSettingService,
                                     JdbcTemplate jdbcTemplate,
                                     BackupService backupService,
                                     SecurityScanService securityScanService) {
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
        this.emailService = emailService;
        this.adminPreferenceRepository = adminPreferenceRepository;
        this.sessionRegistry = sessionRegistry;
        this.applicationSettingService = applicationSettingService;
        this.jdbcTemplate = jdbcTemplate;
        this.backupService = backupService;
        this.securityScanService = securityScanService;
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
    Set<Integer> utilisateursConnectes = sessionRegistry.getAllPrincipals().stream()
        .filter(CustomUserDetails.class::isInstance)
        .map(CustomUserDetails.class::cast)
        .filter(details -> !sessionRegistry.getAllSessions(details, false).isEmpty())
        .map(details -> details.getUtilisateur().getId())
        .collect(java.util.stream.Collectors.toSet());

    model.addAttribute("utilisateurs", tousUtilisateurs);
    model.addAttribute("roles", roleRepository.findAll().stream()
        .filter(r -> "ENCADREUR".equals(r.getLibelle()) || "RESPONSABLE_STAGE".equals(r.getLibelle()))
        .collect(java.util.stream.Collectors.toList()));
    model.addAttribute("services", tousServices);
    model.addAttribute("utilisateursConnectes", utilisateursConnectes);

    long totalUsers = tousUtilisateurs.size();
    long totalResp = tousUtilisateurs.stream().filter(u -> "RESPONSABLE_STAGE".equals(u.getRole().getLibelle())).count();
    long totalEncadreurs = tousUtilisateurs.stream().filter(u -> "ENCADREUR".equals(u.getRole().getLibelle())).count();
    long totalInactifs = tousUtilisateurs.stream()
        .filter(u -> !utilisateursConnectes.contains(u.getId()))
        .count();

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
        boolean connecte = utilisateursConnectes.contains(u.getId());
        m.put("statutCls", connecte ? "actif" : "inactif");
        m.put("statutLabel", connecte ? "Actif" : "Inactif");
        m.put("derniereConnexion", u.getDerniereConnexion() == null
                ? "Jamais"
                : u.getDerniereConnexion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
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
            emailService.envoyerStatutCompte(
                    u.getEmail(), u.getPrenom() + " " + u.getNom(), true);
        });
        return "redirect:/admin/utilisateurs?succes=Compte desactive.";
    }

    @PostMapping("/utilisateurs/reactiver/{id}")
    public String reactiverUtilisateur(@PathVariable Integer id,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        utilisateurRepository.findById(id).ifPresent(u -> {
            u.setStatut(Utilisateur.StatutUtilisateur.actif);
            utilisateurRepository.save(u);
            activityLogService.log("Compte reactive", u.getEmail(), adminName(user));
            emailService.envoyerStatutCompte(
                    u.getEmail(), u.getPrenom() + " " + u.getNom(), false);
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

    @GetMapping("/services/exporter")
    public ResponseEntity<byte[]> exporterServices() {
        List<ServiceEntreprise> services = serviceRepository.findAll();
        List<Stage> stages = stageRepository.findAll();
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <?mso-application progid="Excel.Sheet"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                  xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                  <Worksheet ss:Name="Services"><Table>
                """);
        xml.append(excelRow("Service", "Responsable", "Encadreurs", "Stagiaires", "Capacite"));
        for (ServiceEntreprise service : services) {
            List<Stage> stagesService = stages.stream()
                    .filter(stage -> stage.getService() != null
                            && service.getId().equals(stage.getService().getId()))
                    .toList();
            List<Encadreur> encadreurs = stagesService.stream()
                    .map(Stage::getEncadreur)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toMap(
                                    Encadreur::getId, e -> e, (a, b) -> a,
                                    java.util.LinkedHashMap::new),
                            map -> new ArrayList<>(map.values())));
            String responsable = encadreurs.isEmpty() ? "Non attribue"
                    : encadreurs.get(0).getUtilisateur().getPrenom() + " "
                    + encadreurs.get(0).getUtilisateur().getNom();
            String nomsEncadreurs = encadreurs.stream()
                    .map(e -> e.getUtilisateur().getPrenom() + " "
                            + e.getUtilisateur().getNom())
                    .collect(java.util.stream.Collectors.joining(", "));
            int capacite = Math.max(10, stagesService.size() + 5);
            xml.append(excelRow(service.getNom(), responsable, nomsEncadreurs,
                    String.valueOf(stagesService.size()), String.valueOf(capacite)));
        }
        xml.append("</Table></Worksheet></Workbook>");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"services-" + java.time.LocalDate.now() + ".xls\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(xml.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String excelRow(String... values) {
        StringBuilder row = new StringBuilder("<Row>");
        for (String value : values) {
            row.append("<Cell><Data ss:Type=\"String\">")
                    .append(escapeXml(value))
                    .append("</Data></Cell>");
        }
        return row.append("</Row>").toString();
    }

    private String escapeXml(String value) {
        return value == null ? "" : value.replace("&", "&amp;")
                .replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
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
            m.put("proprietaireNom", "Administration");
            m.put("proprietaireInitiales", "AD");
            m.put("proprietaireRole", "Document externe");
            m.put("proprietaireService", "Administration");
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
    public String supprimerDocument(@PathVariable Integer id,
                                     @RequestParam(defaultValue = "documents") String retour) {
        documentRepository.findById(id).ifPresent(d -> {
            try { Files.deleteIfExists(Paths.get(d.getCheminFichier())); } catch (Exception ignored) {}
            documentRepository.delete(d);
        });
        if ("rapports".equals(retour)) {
            return "redirect:/admin/rapports?succes=Document supprime.";
        }
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

    @GetMapping("/notifications/api")
    @ResponseBody
    public List<Map<String, Object>> notificationsApi() {
        return notificationRepository.findAllByOrderByDateEnvoiDesc().stream()
                .limit(8)
                .map(notification -> Map.<String, Object>of(
                        "objet", notification.getObjet(),
                        "message", notification.getMessage(),
                        "date", notification.getDateEnvoi(),
                        "statut", notification.getStatut()))
                .toList();
    }

    @PostMapping("/notifications/lire")
    @ResponseBody
    public Map<String, Object> marquerNotificationsLues() {
        List<Notification> notifications = notificationRepository.findAllByOrderByDateEnvoiDesc().stream()
                .filter(notification -> !"lue".equalsIgnoreCase(notification.getStatut()))
                .toList();
        notifications.forEach(notification -> notification.setStatut("lue"));
        notificationRepository.saveAll(notifications);
        return Map.of("success", true, "count", 0);
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

    @PostMapping("/journal/ajouter")
    public String ajouterEntreeJournal(@RequestParam String action,
                                       @RequestParam String details,
                                       @AuthenticationPrincipal CustomUserDetails user,
                                       RedirectAttributes ra) {
        activityLogService.log(action.trim(), details.trim(), adminName(user));
        ra.addAttribute("succes", "Entree ajoutee au journal.");
        return "redirect:/admin/journal";
    }

    @PostMapping("/journal/supprimer/{id}")
    public String supprimerEntreeJournal(@PathVariable Integer id,
                                         RedirectAttributes ra) {
        activityLogRepository.deleteById(id);
        ra.addAttribute("succes", "Entree supprimee.");
        return "redirect:/admin/journal";
    }

    @GetMapping("/journal/exporter")
    public ResponseEntity<byte[]> exporterJournal() {
        StringBuilder csv = new StringBuilder("\uFEFFDate;Utilisateur;Action;Details\n");
        activityLogRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(ActivityLog::getDateActivite).reversed())
                .forEach(log -> csv.append(celluleCsv(
                                log.getDateActivite() == null ? "" : log.getDateActivite().toString()))
                        .append(';').append(celluleCsv(log.getUtilisateurNom()))
                        .append(';').append(celluleCsv(log.getAction()))
                        .append(';').append(celluleCsv(log.getDetails())).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"journal-" + java.time.LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ===== PARAMETRES =====
    @GetMapping({"/parametres", "/parametre"})
    public String parametres(Model model,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "parametres");
        AdminPreference preferences = adminPreferenceRepository
                .findByUtilisateurId(user.getUtilisateur().getId())
                .orElseGet(() -> adminPreferenceRepository.save(new AdminPreference(user.getUtilisateur())));
        model.addAttribute("preferences", preferences);
        model.addAttribute("settings", applicationSettingService.getAll());
        Path dataPath = Paths.get("data").toAbsolutePath().normalize();
        try {
            long total = Files.getFileStore(dataPath).getTotalSpace();
            long used = total - Files.getFileStore(dataPath).getUsableSpace();
            model.addAttribute("diskUsed", formatBytes(used));
            model.addAttribute("diskTotal", formatBytes(total));
            model.addAttribute("diskPercent", total == 0 ? 0 : Math.min(100, used * 100 / total));
        } catch (Exception exception) {
            model.addAttribute("diskUsed", "Indisponible");
            model.addAttribute("diskTotal", "Indisponible");
            model.addAttribute("diskPercent", 0);
        }
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("databaseProduct", "H2 " + jdbcTemplate.queryForObject(
                "SELECT H2VERSION()", String.class));
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/parametres";
    }

    @PostMapping("/documents/ajouter")
    public String ajouterDocument(@RequestParam String typeDocument,
                                  @RequestParam MultipartFile fichier,
                                  @AuthenticationPrincipal CustomUserDetails user,
                                  RedirectAttributes redirectAttributes) {
        if (fichier == null || fichier.isEmpty()) {
            redirectAttributes.addFlashAttribute("succes", "Aucun fichier sélectionné.");
            return "redirect:/admin/documents";
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
            redirectAttributes.addFlashAttribute("succes", "Le document n'a pas pu être ajouté.");
            return "redirect:/admin/documents";
        }
        redirectAttributes.addFlashAttribute("succes", "Document ajouté.");
        return "redirect:/admin/documents";
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
    public String sauverParametres(@AuthenticationPrincipal CustomUserDetails user,
                                   @RequestParam(defaultValue = "false") boolean preferenceEmail,
                                   @RequestParam(defaultValue = "false") boolean preferenceSysteme,
                                   @RequestParam(defaultValue = "false") boolean preferenceRappel,
                                   @RequestParam(defaultValue = "false") boolean preferenceSombre,
                                   @RequestParam(defaultValue = "fr") String preferenceLangue,
                                   @RequestParam Map<String, String> formValues,
                                   RedirectAttributes ra) {
        AdminPreference preferences = adminPreferenceRepository
                .findByUtilisateurId(user.getUtilisateur().getId())
                .orElseGet(() -> new AdminPreference(user.getUtilisateur()));
        preferences.setNotificationsEmail(preferenceEmail);
        preferences.setNotificationsSysteme(preferenceSysteme);
        preferences.setRappelTaches(preferenceRappel);
        preferences.setModeSombre(preferenceSombre);
        preferences.setLangue("en".equalsIgnoreCase(preferenceLangue) ? "en" : "fr");
        adminPreferenceRepository.save(preferences);
        Map<String, String> settings = new HashMap<>();
        for (String key : List.of("platformName", "companyName", "contactEmail", "phone",
                "address", "timezone", "academicYear", "startDate", "maxDuration",
                "language", "maxFileSize", "storage", "dateFormat")) {
            if (formValues.containsKey(key)) {
                settings.put(key, formValues.get(key));
            }
        }
        settings.put("emailNotifications",
                String.valueOf(formValues.containsKey("emailNotifications")));
        settings.put("appNotifications",
                String.valueOf(formValues.containsKey("appNotifications")));
        settings.put("maintenanceMode",
                String.valueOf(formValues.containsKey("maintenanceMode")));
        applicationSettingService.save(settings);
        activityLogService.log("Parametres globaux modifies",
                settings.keySet().toString(), adminName(user));
        ra.addAttribute("succes", "Préférences enregistrées.");
        return "redirect:/admin/parametres";
    }

    @PostMapping("/parametres/reinitialiser")
    public String reinitialiserParametres(@AuthenticationPrincipal CustomUserDetails user,
                                          RedirectAttributes ra) {
        applicationSettingService.reset();
        activityLogService.log("Parametres reinitialises",
                "Valeurs globales restaurees", adminName(user));
        ra.addAttribute("succes", "Parametres globaux reinitialises.");
        return "redirect:/admin/parametres";
    }

    @PostMapping("/parametres/maintenance/{action}")
    public String maintenance(@PathVariable String action,
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes ra) {
        String message;
        try {
            switch (action) {
                case "cache" -> {
                    Path temp = Paths.get(System.getProperty("java.io.tmpdir"), "gestion-stages");
                    if (Files.exists(temp)) {
                        try (var paths = Files.walk(temp)) {
                            paths.sorted(java.util.Comparator.reverseOrder())
                                    .filter(path -> !path.equals(temp))
                                    .forEach(path -> {
                                        try {
                                            Files.deleteIfExists(path);
                                        } catch (Exception ignored) {
                                        }
                                    });
                        }
                    }
                    message = "Cache temporaire vide.";
                }
                case "database" -> {
                    jdbcTemplate.execute("ANALYZE");
                    jdbcTemplate.execute("CHECKPOINT");
                    message = "Statistiques et fichiers de la base optimises.";
                }
                case "files" -> {
                    long manquants = documentRepository.findAll().stream()
                            .filter(document -> document.getCheminFichier() == null
                                    || !Files.exists(Paths.get(document.getCheminFichier())))
                            .count();
                    message = manquants == 0
                            ? "Verification terminee : tous les fichiers references existent."
                            : "Verification terminee : " + manquants + " fichier(s) manquant(s).";
                }
                default -> throw new IllegalArgumentException("Action inconnue");
            }
            activityLogService.log("Maintenance " + action, message, adminName(user));
            ra.addAttribute("succes", message);
        } catch (Exception exception) {
            ra.addAttribute("succes", "Operation de maintenance impossible : "
                    + exception.getMessage());
        }
        return "redirect:/admin/parametres";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.1f Mo", bytes / (1024d * 1024d));
        }
        return String.format("%.1f Go", bytes / (1024d * 1024d * 1024d));
    }

    // ===== RAPPORTS =====
    @GetMapping("/rapports")
    public String rapports(Model model) {
        model.addAttribute("activePage", "rapports");
        model.addAttribute("totalStagiaires", stagiaireRepository.count());
        model.addAttribute("totalStages", stageRepository.count());
        model.addAttribute("stagesTermines", stageRepository.findByStatut(Stage.StatutStage.termine).size());
        model.addAttribute("totalDocuments", documentRepository.count());

        List<String> typesAdministratifs = List.of(
                "rapport_hebdomadaire", "rapport_final", "fiche_note", "attestation");
        model.addAttribute("documentsExportes", documentRepository.findAll().stream()
                .filter(document -> typesAdministratifs.contains(document.getTypeDocument()))
                .sorted(java.util.Comparator.comparing(Document::getDateDepot).reversed())
                .toList());
        return "admin/rapports";
    }

    @GetMapping("/rapports/exporter")
    public ResponseEntity<byte[]> exporterRapports() {
        List<String> typesAdministratifs = List.of(
                "rapport_hebdomadaire", "rapport_final", "fiche_note", "attestation");
        List<Document> documents = documentRepository.findAll().stream()
                .filter(document -> typesAdministratifs.contains(document.getTypeDocument()))
                .sorted(java.util.Comparator.comparing(Document::getDateDepot).reversed())
                .toList();

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Document;Propriétaire;Type;Date de dépôt;Taille (octets);Statut\n");
        for (Document document : documents) {
            String proprietaire = "Administration";
            if (document.getStage() != null && document.getStage().getStagiaire() != null) {
                Utilisateur utilisateur = document.getStage().getStagiaire().getUtilisateur();
                proprietaire = utilisateur.getPrenom() + " " + utilisateur.getNom();
            } else if (document.getDemandeStage() != null) {
                proprietaire = document.getDemandeStage().getPrenom() + " "
                        + document.getDemandeStage().getNom();
            }
            csv.append(celluleCsv(document.getNomFichier())).append(';')
                    .append(celluleCsv(proprietaire)).append(';')
                    .append(celluleCsv(document.getTypeDocument())).append(';')
                    .append(celluleCsv(document.getDateDepot() == null ? "" : document.getDateDepot().toString())).append(';')
                    .append(document.getTailleOctets() == null ? 0 : document.getTailleOctets()).append(';')
                    .append(celluleCsv(document.getStatut()))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"rapports-administratifs-"
                                + java.time.LocalDate.now() + ".xls\"")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String celluleCsv(String valeur) {
        String contenu = valeur == null ? "" : valeur.replace("\"", "\"\"");
        return "\"" + contenu + "\"";
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

        try {
            List<Path> backups = backupService.list();
            List<Map<String, Object>> fileHistory = backups.stream().map(path -> {
                Map<String, Object> row = new HashMap<>();
                try {
                    LocalDateTime date = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            java.time.ZoneId.systemDefault());
                    row.put("date", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    row.put("time", date.format(DateTimeFormatter.ofPattern("HH:mm")));
                    row.put("size", formatBytes(Files.size(path)));
                } catch (Exception exception) {
                    row.put("date", "—");
                    row.put("time", "—");
                    row.put("size", "—");
                }
                row.put("fileName", path.getFileName().toString());
                row.put("typeCls", "manuel");
                row.put("typeLabel", "Manuelle");
                row.put("destination", "Serveur local");
                row.put("statutCls", "reussie");
                row.put("statutLabel", "Réussie");
                return row;
            }).toList();
            model.addAttribute("backupHistory", fileHistory);
            model.addAttribute("totalBackups", backups.size());
            if (!backups.isEmpty()) {
                LocalDateTime last = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(backups.get(0)).toInstant(),
                        java.time.ZoneId.systemDefault());
                model.addAttribute("lastBackupDate",
                        last.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                model.addAttribute("lastBackupTime",
                        last.format(DateTimeFormatter.ofPattern("HH:mm")));
                long totalSize = backups.stream().mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (Exception exception) {
                        return 0;
                    }
                }).sum();
                model.addAttribute("totalBackupSize", formatBytes(totalSize));
            }
        } catch (Exception exception) {
            model.addAttribute("backupHistory", List.of());
        }

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
    public String lancerSauvegarde(@RequestParam(required = false) String name,
                                   @AuthenticationPrincipal CustomUserDetails user,
                                   RedirectAttributes ra) {
        try {
            Path backup = backupService.create(name);
            activityLogService.log("Sauvegarde creee",
                    backup.getFileName().toString(), adminName(user));
            ra.addAttribute("succes", "Sauvegarde creee avec succes.");
        } catch (Exception exception) {
            activityLogService.log("Echec sauvegarde", exception.getMessage(), adminName(user));
            ra.addAttribute("succes", "Echec de la sauvegarde : " + exception.getMessage());
        }
        return "redirect:/admin/sauvegarde";
    }

    @GetMapping("/sauvegarde/telecharger/{fileName:.+}")
    public ResponseEntity<byte[]> telechargerSauvegarde(@PathVariable String fileName) throws Exception {
        Path file = backupService.get(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(Files.readAllBytes(file));
    }

    @PostMapping("/sauvegarde/supprimer/{fileName:.+}")
    public String supprimerSauvegarde(@PathVariable String fileName,
                                      @AuthenticationPrincipal CustomUserDetails user,
                                      RedirectAttributes ra) {
        try {
            backupService.delete(fileName);
            activityLogService.log("Sauvegarde supprimee", fileName, adminName(user));
            ra.addAttribute("succes", "Sauvegarde supprimee.");
        } catch (Exception exception) {
            ra.addAttribute("succes", "Suppression impossible : " + exception.getMessage());
        }
        return "redirect:/admin/sauvegarde";
    }

    @PostMapping("/sauvegarde/restaurer/{fileName:.+}")
    public String restaurerSauvegarde(@PathVariable String fileName,
                                      @RequestParam String confirmation,
                                      @AuthenticationPrincipal CustomUserDetails user,
                                      RedirectAttributes ra) {
        if (!"RESTAURER".equals(confirmation)) {
            ra.addAttribute("succes",
                    "Restauration annulee : confirmation RESTAURER requise.");
            return "redirect:/admin/sauvegarde";
        }
        try {
            BackupService.RestoreResult result = backupService.restore(fileName);
            activityLogService.log("Sauvegarde restauree",
                    result.restoredBackup().getFileName()
                            + " | Copie de securite: "
                            + result.safetyBackup().getFileName(),
                    adminName(user));
            ra.addAttribute("succes",
                    "Restauration terminee. Une copie de securite a ete conservee.");
        } catch (Exception exception) {
            ra.addAttribute("succes",
                    "Restauration impossible; retour arriere tente : "
                            + exception.getMessage());
        }
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
        long activeSessionCount = sessionRegistry.getAllPrincipals().stream()
                .mapToLong(principal -> sessionRegistry.getAllSessions(principal, false).size())
                .sum();
        model.addAttribute("activeSessions", activeSessionCount);
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

        List<Map<String,Object>> sessionsList = sessionRegistry.getAllPrincipals().stream()
                .filter(CustomUserDetails.class::isInstance)
                .map(CustomUserDetails.class::cast)
                .flatMap(details -> sessionRegistry.getAllSessions(details, false).stream()
                        .map(session -> {
                            Utilisateur utilisateur = details.getUtilisateur();
                            Map<String, Object> item = new HashMap<>();
                            item.put("sessionId", session.getSessionId());
                            item.put("initiale", utilisateur.getPrenom().substring(0, 1).toUpperCase()
                                    + utilisateur.getNom().substring(0, 1).toUpperCase());
                            item.put("nom", utilisateur.getPrenom() + " " + utilisateur.getNom());
                            item.put("appareil", utilisateur.getEmail());
                            item.put("ip", "Session web");
                            long minutes = Math.max(0, java.time.Duration.between(
                                    session.getLastRequest().toInstant(),
                                    java.time.Instant.now()).toMinutes());
                            item.put("lastActivity", minutes == 0
                                    ? "A l'instant" : "Il y a " + minutes + " min");
                            return item;
                        }))
                .toList();
        model.addAttribute("activeSessionsList", sessionsList);

        // Comptes verrouillés (utilisateurs inactifs)
        List<Map<String,Object>> lockedList = allUsers.stream()
            .filter(u -> u.getStatut() == Utilisateur.StatutUtilisateur.inactif)
             .map(u -> {
                 Map<String,Object> m = new HashMap<>();
                 m.put("id", u.getId());
                m.put("initiale", u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase());
                m.put("nom", u.getPrenom() + " " + u.getNom());
                m.put("attempts", 5);
                m.put("date", u.getDateCreation() != null ? sdfDate.format(java.sql.Timestamp.valueOf(u.getDateCreation())) : "—");
                m.put("time", "—");
                return m;
            }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("lockedAccountsList", lockedList);

        long twoFactorEnabled = allUsers.stream()
                .filter(Utilisateur::isTwoFactorEnabled)
                .count();
        model.addAttribute("twoFactorEnabled", twoFactorEnabled);
        model.addAttribute("twoFactorDisabled", totalUsers - twoFactorEnabled);
        model.addAttribute("twoFactorPct",
                totalUsers == 0 ? 0 : twoFactorEnabled * 100 / totalUsers);
        model.addAttribute("scanReport", securityScanService.scan());

        return "admin/securite";
    }

    @PostMapping("/securite/comptes/{id}/deverrouiller")
    public String deverrouillerCompte(@PathVariable Integer id,
                                      @AuthenticationPrincipal CustomUserDetails user,
                                      RedirectAttributes ra) {
        utilisateurRepository.findById(id).ifPresent(utilisateur -> {
            utilisateur.setStatut(Utilisateur.StatutUtilisateur.actif);
            utilisateurRepository.save(utilisateur);
            activityLogService.log("Compte deverrouille", utilisateur.getEmail(), adminName(user));
        });
        ra.addAttribute("succes", "Compte deverrouille.");
        return "redirect:/admin/securite";
    }

    @PostMapping("/securite/comptes/{id}/reinitialiser")
    public String reinitialiserCompteSecurite(@PathVariable Integer id,
                                               @AuthenticationPrincipal CustomUserDetails user,
                                               RedirectAttributes ra) {
        utilisateurRepository.findById(id).ifPresent(utilisateur -> {
            utilisateur.setMotDePasse(passwordEncoder.encode("dta2026"));
            utilisateurRepository.save(utilisateur);
            activityLogService.log("Mot de passe reinitialise",
                    utilisateur.getEmail(), adminName(user));
        });
        ra.addAttribute("succes", "Mot de passe reinitialise a dta2026.");
        return "redirect:/admin/securite";
    }

    @PostMapping("/securite/sessions/{sessionId}/fermer")
    public String fermerSession(@PathVariable String sessionId,
                                @AuthenticationPrincipal CustomUserDetails user,
                                RedirectAttributes ra) {
        var information = sessionRegistry.getSessionInformation(sessionId);
        if (information != null) {
            information.expireNow();
            activityLogService.log("Session fermee", sessionId, adminName(user));
        }
        ra.addAttribute("succes", "Session fermee.");
        return "redirect:/admin/securite";
    }

    @GetMapping("/securite/exporter")
    public ResponseEntity<byte[]> exporterSecurite() {
        StringBuilder csv = new StringBuilder("\uFEFFDate;Utilisateur;Action;Details\n");
        activityLogRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(ActivityLog::getDateActivite).reversed())
                .forEach(log -> csv.append(celluleCsv(
                                log.getDateActivite() == null ? "" : log.getDateActivite().toString()))
                        .append(';').append(celluleCsv(log.getUtilisateurNom()))
                        .append(';').append(celluleCsv(log.getAction()))
                        .append(';').append(celluleCsv(log.getDetails())).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"securite-" + java.time.LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/securite/scanner")
    public String scannerSecurite(@AuthenticationPrincipal CustomUserDetails user,
                                  RedirectAttributes ra) {
        SecurityScanService.Report report = securityScanService.scan();
        activityLogService.log("Scan de securite",
                "Score " + report.score() + "/100, "
                        + report.findings().size() + " constat(s)",
                adminName(user));
        ra.addAttribute("succes",
                "Scan termine : score " + report.score() + "/100.");
        return "redirect:/admin/securite";
    }

    @PostMapping("/securite/2fa/exiger")
    public String exigerDeuxFacteurs(@AuthenticationPrincipal CustomUserDetails user,
                                     RedirectAttributes ra) {
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        utilisateurs.forEach(utilisateur -> utilisateur.setTwoFactorRequired(true));
        utilisateurRepository.saveAll(utilisateurs);
        activityLogService.log("2FA exigee",
                utilisateurs.size() + " compte(s)", adminName(user));
        ra.addAttribute("succes",
                "La double authentification sera configuree a la prochaine connexion.");
        return "redirect:/admin/securite";
    }

    @PostMapping("/securite/2fa/reinitialiser-mon-compte")
    public String reinitialiserMaDeuxFacteurs(
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes ra) {
        Utilisateur utilisateur = utilisateurRepository
                .findById(user.getUtilisateur().getId()).orElseThrow();
        utilisateur.setTwoFactorEnabled(false);
        utilisateur.setTwoFactorRequired(true);
        utilisateur.setTwoFactorSecret(null);
        utilisateurRepository.save(utilisateur);
        activityLogService.log("Secret 2FA reinitialise",
                utilisateur.getEmail(), adminName(user));
        ra.addAttribute("succes",
                "Votre 2FA devra etre reconfiguree a votre prochaine connexion.");
        return "redirect:/admin/securite";
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
