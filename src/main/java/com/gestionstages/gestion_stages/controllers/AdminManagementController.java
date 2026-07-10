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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

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
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("totalUsers", utilisateurRepository.count());
        model.addAttribute("totalAdmin", utilisateurRepository.findByRole_Libelle("ADMINISTRATEUR").size());
        model.addAttribute("totalResp", utilisateurRepository.findByRole_Libelle("RESPONSABLE_STAGE").size());
        model.addAttribute("totalStag", utilisateurRepository.findByRole_Libelle("STAGIAIRE").size());
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
    @GetMapping("/services")
    public String services(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "services");
        model.addAttribute("services", serviceRepository.findAll());
        model.addAttribute("totalServices", serviceRepository.count());
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
        List<Document> docs = documentRepository.findAll();
        model.addAttribute("documents", docs);
        model.addAttribute("totalDocs", docs.size());
        model.addAttribute("totalRapports", docs.stream().filter(d -> "rapport_final".equals(d.getTypeDocument())).count());
        model.addAttribute("totalConventions", docs.stream().filter(d -> "convention".equals(d.getTypeDocument())).count());
        model.addAttribute("totalArchives", docs.stream().filter(d -> "archive".equals(d.getStatut())).count());
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
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/notifications";
    }

    @PostMapping("/notifications/envoyer")
    public String envoyerNotification(@RequestParam String objet, @RequestParam String message,
                                         @RequestParam String destinataireType, @RequestParam String priorite,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        Notification n = new Notification(objet, message, destinataireType, priorite, adminName(user));
        notificationRepository.save(n);
        activityLogService.log("Notification envoyee", objet + " -> " + destinataireType, adminName(user));
        return "redirect:/admin/notifications?succes=Notification envoyee.";
    }

    // ===== JOURNAL =====
    @GetMapping("/journal")
    public String journal(Model model) {
        model.addAttribute("activePage", "journal");
        model.addAttribute("activites", activityLogRepository.findTop50ByOrderByDateActiviteDesc());
        return "admin/journal";
    }

    // ===== PARAMETRES =====
    @GetMapping("/parametres")
    public String parametres(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "parametres");
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/parametres";
    }

    @PostMapping("/parametres")
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
        return "admin/rapports";
    }

    // ===== SAUVEGARDE =====
    @GetMapping("/sauvegarde")
    public String sauvegarde(Model model, @RequestParam(required = false) String succes) {
        model.addAttribute("activePage", "sauvegarde");
        if (succes != null) model.addAttribute("succes", succes);
        return "admin/sauvegarde";
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
        model.addAttribute("roles", roleRepository.findAll());
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
        return "admin/securite";
    }
}
