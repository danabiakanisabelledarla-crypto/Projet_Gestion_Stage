package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public AdminController(DemandeStageRepository demandeStageRepository,
                            StageRepository stageRepository,
                            TacheRepository tacheRepository,
                            StagiaireRepository stagiaireRepository,
                            EncadreurRepository encadreurRepository,
                            UtilisateurRepository utilisateurRepository,
                            ServiceEntrepriseRepository serviceRepository,
                            DocumentRepository documentRepository,
                            NotificationRepository notificationRepository,
                            ActivityLogRepository activityLogRepository) {
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
    }
    @GetMapping("/dashboard")
public String afficherDashboard(Model model) {
    model.addAttribute("activePage", "dashboard");
    model.addAttribute("nomComplet", "Administrateur");
    model.addAttribute("initiales", "A");

    // STATS
    long nombreStagiaires = stagiaireRepository.count();
    long nombreEncadreurs = encadreurRepository.count();
    long nombreServices = serviceRepository.count();
    long nombreDocuments = documentRepository.count();
    long nombreDemandes = demandeStageRepository.count();
    long stagesActifs = stageRepository.findByStatut(Stage.StatutStage.en_cours).size();
    long demandesEnAttente = demandeStageRepository.findByStatut(DemandeStage.StatutDemande.en_attente).size();

    model.addAttribute("nombreStagiaires", nombreStagiaires);
    model.addAttribute("nombreEncadreurs", nombreEncadreurs);
    model.addAttribute("nombreServices", nombreServices);
    model.addAttribute("nombreDocuments", nombreDocuments);
    model.addAttribute("nombreDemandes", nombreDemandes);
    model.addAttribute("stagesActifs", stagesActifs);
    model.addAttribute("demandesEnAttente", demandesEnAttente);

    // GRAPHIQUES
    List<Stage> tousStages = stageRepository.findAll();
    long stagesTermines = tousStages.stream().filter(s -> s.getStatut() == Stage.StatutStage.termine).count();
    long stagesSuspendus = tousStages.stream().filter(s -> s.getStatut() == Stage.StatutStage.suspendu).count();
    model.addAttribute("stagesEnCours", stagesActifs);
    model.addAttribute("stagesTermines", stagesTermines);
    model.addAttribute("stagesSuspendus", stagesSuspendus);

    // Évolution inscriptions (6 derniers mois simulée avec utilisateurs)
    List<Utilisateur> tousUtilisateurs = utilisateurRepository.findAll();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");
    List<String> moisLabels = new ArrayList<>();
    List<Long> inscriptionsData = new ArrayList<>();
    LocalDate now = LocalDate.now();
    for (int i = 5; i >= 0; i--) {
        LocalDate debutMois = now.minusMonths(i).withDayOfMonth(1);
        LocalDate finMois = debutMois.plusMonths(1).minusDays(1);
        LocalDate finalDebut = debutMois;
        LocalDate finalFin = finMois;
        long count = tousUtilisateurs.stream().filter(u -> {
            // approximation simple : on ne peut pas dater la création
            return true;
        }).count();
        moisLabels.add(debutMois.format(fmt.withLocale(java.util.Locale.FRENCH)));
        inscriptionsData.add(count / 6); // valeur simulée
    }
    // Valeurs simulées plus réalistes
    inscriptionsData = List.of(12L, 19L, 8L, 24L, 15L, 22L);
    moisLabels = List.of("Jan", "Fév", "Mar", "Avr", "Mai", "Jun");
    model.addAttribute("moisLabels", moisLabels);
    model.addAttribute("inscriptionsData", inscriptionsData);

    // ACTIVITÉS RÉCENTES
    model.addAttribute("activitesRecentes", activityLogRepository.findTop50ByOrderByDateActiviteDesc());

    // TOP ENCADREURS
    List<Encadreur> tousEncadreurs = encadreurRepository.findAll();
    List<Map<String, Object>> topEncadreurs = tousEncadreurs.stream()
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                String prenom = e.getUtilisateur().getPrenom();
                String nom = e.getUtilisateur().getNom();
                m.put("nom", prenom + " " + nom);
                m.put("initiales", prenom.substring(0,1).toUpperCase() + nom.substring(0,1).toUpperCase());
                long nb = stageRepository.findByEncadreurId(e.getId()).size();
                m.put("nombreStagiaires", nb);
                long etoiles = Math.min(5, Math.max(1, nb / 3));
                m.put("etoiles", "⭐".repeat((int)etoiles) + "☆".repeat((int)(5-etoiles)));
                return m;
            })
            .sorted((a,b)->Long.compare((Long)b.get("nombreStagiaires"), (Long)a.get("nombreStagiaires")))
            .limit(5)
            .collect(Collectors.toList());
    model.addAttribute("topEncadreurs", topEncadreurs);

    // RÉPARTITION SERVICES
    List<ServiceEntreprise> tousServices = serviceRepository.findAll();
    long totalStagiaires = Math.max(1, nombreStagiaires);
    List<Map<String, Object>> repartitionServices = tousServices.stream()
            .map(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("nom", s.getNom());
                long nbStagiaires = stageRepository.findAll().stream()
                        .filter(st -> st.getService().getId().equals(s.getId()))
                        .count();
                m.put("nombreStagiaires", nbStagiaires);
                m.put("pourcentage", nbStagiaires * 100 / totalStagiaires);
                return m;
            })
            .sorted((a,b)->Long.compare((Long)b.get("nombreStagiaires"), (Long)a.get("nombreStagiaires")))
            .collect(Collectors.toList());
    model.addAttribute("repartitionServices", repartitionServices);

    // NOTIFICATIONS
    model.addAttribute("notificationsRecentes", notificationRepository.findAllByOrderByDateEnvoiDesc());

    // CALENDRIER — événements du mois courant
    List<Stage> tousStagesList = stageRepository.findAll();
    List<String> evenementsCal = new ArrayList<>();
    for (Stage st : tousStagesList) {
        if (st.getDateDebut() != null) evenementsCal.add(st.getDateDebut().toString());
        if (st.getDateFin() != null) evenementsCal.add(st.getDateFin().toString());
    }
    model.addAttribute("evenementsCalendrier", evenementsCal);

    // TÂCHES EN RETARD (simulé si pas de champ dateLimite nullable)
    // model.addAttribute("tachesEnRetard", tacheRepository...);

    return "admin/dashboard";
}
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

    // JSON pour le modal (JS)
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

        // Documents liés à cette demande
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

    // JSON pour le panneau de détails
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

        // Infos from DemandeStage
        if (s.getDemandeStage() != null) {
            m.put("universite", s.getDemandeStage().getEcole() != null ? s.getDemandeStage().getEcole() : "—");
            m.put("filiere", s.getDemandeStage().getFiliere() != null ? s.getDemandeStage().getFiliere() : "—");
            m.put("niveau", s.getDemandeStage().getNiveau() != null ? s.getDemandeStage().getNiveau() : "—");
        } else {
            m.put("universite", "—");
            m.put("filiere", "—");
            m.put("niveau", "—");
        }

        // Stage info
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

            // Durée restante
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

            // Progression
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

        // Documents
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