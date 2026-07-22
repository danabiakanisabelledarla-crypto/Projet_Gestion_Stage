package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.dto.EvenementPlanning;
import java.util.Comparator;

import com.gestionstages.gestion_stages.entities.*;
import com.gestionstages.gestion_stages.repositories.*;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.gestionstages.gestion_stages.repositories.EvaluationRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
@RequestMapping("/stagiaire")
public class StagiaireController {

    private final StageRepository stageRepository;
private final TacheRepository tacheRepository;
private final LivrableRepository livrableRepository;
private final JournalBordRepository journalBordRepository;
private final StagiaireRepository stagiaireRepository;
private final DocumentRepository documentRepository;
private final ObjectifRepository objectifRepository;
private final EvaluationRepository evaluationRepository;
private final EvenementPersonnelRepository evenementPersonnelRepository;

private final UtilisateurRepository utilisateurRepository;
private final PasswordEncoder passwordEncoder;
private final ConversationRepository conversationRepository;
private final MessageRepository messageRepository;

private static final String DOSSIER_UPLOAD = "uploads/";

public StagiaireController(StageRepository stageRepository,
                           TacheRepository tacheRepository,
                           LivrableRepository livrableRepository,
                           JournalBordRepository journalBordRepository,
                           StagiaireRepository stagiaireRepository,
                           DocumentRepository documentRepository,
                           ObjectifRepository objectifRepository,
                           EvaluationRepository evaluationRepository,
                           EvenementPersonnelRepository evenementPersonnelRepository,
                           UtilisateurRepository utilisateurRepository,
                           PasswordEncoder passwordEncoder,
                           ConversationRepository conversationRepository,
                           MessageRepository messageRepository) {

    this.stageRepository = stageRepository;
    this.tacheRepository = tacheRepository;
    this.livrableRepository = livrableRepository;
    this.journalBordRepository = journalBordRepository;
    this.stagiaireRepository = stagiaireRepository;
    this.documentRepository = documentRepository;
    this.objectifRepository = objectifRepository;
    this.evaluationRepository = evaluationRepository;
    this.evenementPersonnelRepository = evenementPersonnelRepository;
    this.utilisateurRepository = utilisateurRepository;
    this.passwordEncoder = passwordEncoder;
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
}

    private Optional<Stage> getStage(CustomUserDetails userDetails) {
    Integer utilisateurId = userDetails.getUtilisateur().getId();
    return stagiaireRepository.findByUtilisateurId(utilisateurId)
            .flatMap(stagiaire -> stageRepository.findByStagiaireId(stagiaire.getId()));
}
 
@GetMapping("/dashboard")
public String afficherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    String initiales = prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase();

    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", initiales);
    model.addAttribute("activePage", "dashboard");


    if (stageOpt.isPresent()) {
        Stage stage = stageOpt.get();
        model.addAttribute("stage", stage);

        List<Tache> taches = tacheRepository.findByStageId(stage.getId());
        long tachesEnCours = taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.en_cours)
                .count();
        long tachesAFaire = taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.a_faire)
                .count();

        List<Livrable> livrables = new ArrayList<>();
        for (Tache t : taches) {
            livrables.addAll(livrableRepository.findByTacheId(t.getId()));
        }
        long livrablesPending = livrables.stream()
                .filter(l -> l.getStatut() == Livrable.StatutLivrable.depose)
                .count();

        List<Objectif> objectifs = objectifRepository
                .findByStageIdOrderByOrdreAsc(stage.getId());
        long objectifsEnCours = objectifs.stream()
                .filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours)
                .count();

        int totalTaches = taches.size();
        int tachesTerminees = (int) taches.stream()
                .filter(t -> t.getStatut() == Tache.StatutTache.terminee)
                .count();
        int progression = totalTaches > 0
                ? (tachesTerminees * 100 / totalTaches) : 0;

        List<JournalBord> journaux = journalBordRepository
                .findByStageIdOrderByDateActiviteDesc(stage.getId());

        model.addAttribute("nombreTaches", totalTaches);
        model.addAttribute("tachesEnCours", tachesEnCours);
        model.addAttribute("tachesRecentes", taches.stream().limit(3).toList());
        model.addAttribute("nombreLivrables", livrables.size());
        model.addAttribute("livrablesPending", livrablesPending);
        model.addAttribute("nombreObjectifs", objectifs.size());
        model.addAttribute("objectifsEnCours", objectifsEnCours);
        model.addAttribute("progression", progression);
        model.addAttribute("nombreJours", journaux.size());

    } else {
        model.addAttribute("stage", null);
        model.addAttribute("nombreTaches", 0);
        model.addAttribute("tachesEnCours", 0);
        model.addAttribute("tachesRecentes", new ArrayList<>());
        model.addAttribute("nombreLivrables", 0);
        model.addAttribute("livrablesPending", 0);
        model.addAttribute("nombreObjectifs", 0);
        model.addAttribute("objectifsEnCours", 0);
        model.addAttribute("progression", 0);
        model.addAttribute("nombreJours", 0);
    }

    return "stagiaire/dashboard";
}
// dans afficherJournal
    @GetMapping("/journal")
    public String afficherJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   @RequestParam(required = false) String succes) {
        Optional<Stage> stageOpt = getStage(userDetails);
        model.addAttribute("activePage", "journal");
        String prenom = userDetails.getUtilisateur().getPrenom();
        String nom = userDetails.getUtilisateur().getNom();
        model.addAttribute("prenom", prenom);
        model.addAttribute("nomComplet", prenom + " " + nom);
        model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
                + nom.substring(0,1).toUpperCase());

        if (stageOpt.isPresent()) {
            Stage stage = stageOpt.get();
            model.addAttribute("stage", stage);

            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            model.addAttribute("taches", taches);

            List<Livrable> livrables = new ArrayList<>();
            for (Tache t : taches) {
                livrables.addAll(livrableRepository.findByTacheId(t.getId()));
            }
            model.addAttribute("livrables", livrables);
            List<JournalBord> journaux = journalBordRepository
                    .findByStageIdOrderByDateActiviteDesc(stage.getId());
            model.addAttribute("journaux", journaux);
            model.addAttribute("nombreEntrees", journaux.size());
        } else {
            model.addAttribute("journaux", new ArrayList<>());
            model.addAttribute("nombreEntrees", 0);
            model.addAttribute("stage", null);
            model.addAttribute("taches", new ArrayList<>());
            model.addAttribute("livrables", new ArrayList<>());
        }

        if (succes != null) model.addAttribute("succes", succes);
        return "stagiaire/journal";
    }

    @PostMapping("/journal/ajouter")
    public String ajouterJournal(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String dateActivite,
                                  @RequestParam String travauxRealises,
                                  @RequestParam(required = false) String difficultes,
                                  @RequestParam(required = false) String solutions) {
        getStage(userDetails).ifPresent(stage -> {
            JournalBord journal = new JournalBord(stage,
                    LocalDate.parse(dateActivite), travauxRealises);

            journal.setDifficultes(difficultes);
            journal.setObservations(solutions);
            journalBordRepository.save(journal);
            journal.setObservations(solutions);
        });
        return "redirect:/stagiaire/journal?succes=Journal enregistre avec succes.";
    }

    @PostMapping("/livrables/deposer")
    public String deposerLivrable(@AuthenticationPrincipal CustomUserDetails userDetails,
                                @RequestParam String titre,
                                @RequestParam MultipartFile fichier) {
        try {
            Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
            String nomFichier = "livrable_" + userDetails.getUtilisateur().getId() 
                    + "_" + System.currentTimeMillis() + "_" + fichier.getOriginalFilename();
            Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
            Files.write(chemin, fichier.getBytes());

            Optional<Stage> stageOpt = getStage(userDetails);
            if (stageOpt.isPresent()) {
                Stage stage = stageOpt.get();
                Livrable livrable = new Livrable();
                livrable.setTitre(titre);
                livrable.setFichier(chemin.toString());
                livrable.setStage(stage);
                livrable.setStatut(Livrable.StatutLivrable.depose);
                livrable.setDateDepot(java.time.LocalDateTime.now());
                livrableRepository.save(livrable);
            }
        } catch (IOException e) {
            System.err.println("Erreur upload livrable : " + e.getMessage());
            return "redirect:/stagiaire/livrables?erreur=Erreur lors du depot du document.";
        }
        return "redirect:/stagiaire/livrables?succes=Document depose avec succes.";
    }

@GetMapping("/taches")
public String afficherTaches(@AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Tache> taches = new ArrayList<>();

    model.addAttribute("activePage", "taches");
    if (stageOpt.isPresent()) {
        taches = tacheRepository.findByStageId(stageOpt.get().getId());
    }
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
        + nom.substring(0,1).toUpperCase());
    model.addAttribute("taches", taches);
    long tachesAFaire = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.a_faire).count();
    long tachesEnCours = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.en_cours).count();
    long tachesTerminees = taches.stream().filter(t -> t.getStatut() == Tache.StatutTache.terminee).count();
    model.addAttribute("tachesAFaire", tachesAFaire);
    model.addAttribute("tachesEnCours", tachesEnCours);
    model.addAttribute("tachesTerminees", tachesTerminees);
    return "stagiaire/taches";

}

@GetMapping("/livrables")
public String afficherLivrables(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 Model model) {
    Optional<Stage> stageOpt = getStage(userDetails);
    List<Livrable> livrables = new ArrayList<>();

    model.addAttribute("activePage", "livrables");

    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());

    if (stageOpt.isPresent()) {
        List<Tache> taches = tacheRepository.findByStageId(stageOpt.get().getId());
        for (Tache t : taches) {
            livrables.addAll(livrableRepository.findByTacheId(t.getId()));
        }
    }
    model.addAttribute("livrables", livrables);
    model.addAttribute("nombreLivrables", livrables.size());
    return "stagiaire/livrables";
}

@GetMapping("/rapport")
public String afficherRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model,
                               @RequestParam(required = false) String succes) {
    
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    model.addAttribute("activePage", "rapport");
    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());
    
    stageOpt.ifPresent(stage -> {
        List<Document> rapports = documentRepository.findByStageId(stage.getId()).stream()
                .filter(d -> "rapport_final".equals(d.getTypeDocument()))
                .toList();
        
        // Trier manuellement si la méthode existe
        model.addAttribute("rapports", rapports);
        
        if (!rapports.isEmpty()) {
            model.addAttribute("dernierRapport", rapports.get(0));
            model.addAttribute("nombreVersions", rapports.size());
        }
    });
    
    if (succes != null) model.addAttribute("succes", succes);
    return "stagiaire/rapport";
}

@PostMapping("/rapport/deposer")
public String deposerRapport(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam String titre,
                              @RequestParam MultipartFile fichier) {
    Optional<Stage> stageOpt = getStage(userDetails);
    if (stageOpt.isEmpty()) {
        return "redirect:/stagiaire/rapport";
    }
    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));
        String nomFichier = "rapport_" + userDetails.getUtilisateur().getId()
                + "_" + fichier.getOriginalFilename();
        Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
        Files.write(chemin, fichier.getBytes());

        Document document = new Document(
                fichier.getOriginalFilename(), "rapport_final", chemin.toString());
        document.setStage(stageOpt.get());
        documentRepository.save(document);
    } catch (IOException e) {
        System.err.println("Erreur upload rapport : " + e.getMessage());
        return "redirect:/stagiaire/rapport?succes=Erreur lors du depot du rapport.";
    }
    return "redirect:/stagiaire/rapport?succes=Rapport depose avec succes.";
}

@GetMapping("/profil")
public String afficherProfilStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "profil");
    model.addAttribute("utilisateur", userDetails.getUtilisateur());
    stagiaireRepository.findByUtilisateurId(userDetails.getUtilisateur().getId())
            .ifPresent(s -> model.addAttribute("stagiaire", s));
    model.addAttribute("stage", getStage(userDetails).orElse(null));
    return "stagiaire/profil";
}



@GetMapping("/objectifs")
public String afficherObjectifsStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "objectifs");
    Optional<Stage> stageOpt = getStage(userDetails);
    Stage stage = stageOpt.orElse(null);
    model.addAttribute("stage", stage);
    model.addAttribute("activePage", "objectifs");

    String prenom = userDetails.getUtilisateur().getPrenom();
    String nom = userDetails.getUtilisateur().getNom();
    model.addAttribute("prenom", prenom);
    model.addAttribute("nomComplet", prenom + " " + nom);
    model.addAttribute("initiales", prenom.substring(0,1).toUpperCase()
            + nom.substring(0,1).toUpperCase());

    List<Objectif> objectifs = stageOpt.isPresent()
            ? objectifRepository.findByStageIdOrderByOrdreAsc(stageOpt.get().getId())
            : new ArrayList<>();

    model.addAttribute("objectifs", objectifs);
    long total = objectifs.size();
    long atteints = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.atteint).count();
    long enCours = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.en_cours).count();
    long enAttente = objectifs.stream().filter(o -> o.getStatut() == Objectif.StatutObjectif.non_commence).count();

    model.addAttribute("nombreObjectifs", total);
    model.addAttribute("objectifsAtteints", atteints);
    model.addAttribute("objectifsEnCours", enCours);
    model.addAttribute("objectifsNonCommences", enAttente);

    double progressionMoyenne = objectifs.stream().mapToInt(Objectif::getProgression).average().orElse(0);
    model.addAttribute("progressionMoyenne", (int) Math.round(progressionMoyenne));

    LocalDate aujourdHui = LocalDate.now();
    List<Objectif> echeancesProches = objectifs.stream()
            .filter(o -> o.getDateLimite() != null
                    && !o.getDateLimite().isBefore(aujourdHui)
                    && o.getDateLimite().isBefore(aujourdHui.plusDays(7))
                    && o.getStatut() != Objectif.StatutObjectif.atteint)
            .toList();
    model.addAttribute("echeancesProches", echeancesProches);

    if (stage != null && stage.getEncadreur() != null) {
        model.addAttribute("encadreurNom", stage.getEncadreur().getUtilisateur().getPrenom()
                + " " + stage.getEncadreur().getUtilisateur().getNom());
        model.addAttribute("serviceNom", stage.getService().getNom());
    }

    return "stagiaire/objectifs";
}

@PostMapping("/objectifs/creer")
public String creerObjectif(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam String libelle,
                            @RequestParam(required = false) String description,
                            @RequestParam(defaultValue = "moyenne") String priorite,
                            @RequestParam(required = false) String dateLimite) {
    Optional<Stage> stageOpt = getStage(userDetails);
    if (stageOpt.isPresent()) {
        Objectif obj = new Objectif();
        obj.setStage(stageOpt.get());
        obj.setLibelle(libelle);
        obj.setDescription(description);
        obj.setPriorite(Objectif.Priorite.valueOf(priorite));
        if (dateLimite != null && !dateLimite.isEmpty())
            obj.setDateLimite(LocalDate.parse(dateLimite));
        obj.setOrdre(0);
        obj.setProgression(0);
        obj.setStatut(Objectif.StatutObjectif.non_commence);
        objectifRepository.save(obj);
    }
    return "redirect:/stagiaire/objectifs";
}

@PostMapping("/objectifs/statut")
public String changerStatutObjectif(@RequestParam Integer id,
                                    @RequestParam String statut) {
    Objectif obj = objectifRepository.findById(id).orElse(null);
    if (obj != null) {
        obj.setStatut(Objectif.StatutObjectif.valueOf(statut));
        if (statut.equals("atteint")) obj.setProgression(100);
        objectifRepository.save(obj);
    }
    return "redirect:/stagiaire/objectifs";
}

@PostMapping("/objectifs/supprimer")
public String supprimerObjectif(@RequestParam Integer id) {
    objectifRepository.deleteById(id);
    return "redirect:/stagiaire/objectifs";
}

@GetMapping("/planning")
public String afficherPlanningStagiaire(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    model.addAttribute("activePage", "planning");
    Optional<Stage> stageOpt = getStage(userDetails);
    model.addAttribute("stage", stageOpt.orElse(null));

    List<EvenementPlanning> evenements = new ArrayList<>();

        if (stageOpt.isPresent()) {
            Stage stage = stageOpt.get();

            List<Tache> taches = tacheRepository.findByStageId(stage.getId());
            for (Tache t : taches) {
                if (t.getDateLimite() != null) {
                    evenements.add(new EvenementPlanning(
                            t.getDateLimite(), "Echeance : " + t.getTitre(), "tache"));
                }
            }

            List<Evaluation> evaluations = evaluationRepository.findByStageId(stage.getId());
            for (Evaluation e : evaluations) {
                evenements.add(new EvenementPlanning(
                        e.getDateEvaluation(), "Evaluation (" + e.getTypeEvaluation() + ")", "evaluation"));
            }
            if (stage.getDateFin() != null) {
                evenements.add(new EvenementPlanning(stage.getDateFin(), "Fin du stage", "fin_stage"));
            }

            List<EvenementPersonnel> evenementsPerso = evenementPersonnelRepository.findByStageId(stage.getId());
            for (EvenementPersonnel ep : evenementsPerso) {
                evenements.add(new EvenementPlanning(ep.getDate(), ep.getMotif(), ep.getTypeCouleur()));
            }
        }

    evenements.sort(Comparator.comparing(EvenementPlanning::getDate));
    model.addAttribute("evenements", evenements);

    LocalDate aujourdHui = LocalDate.now();
    model.addAttribute("evenementsAvenir", evenements.stream()
            .filter(e -> !e.getDate().isBefore(aujourdHui))
            .toList());

    return "stagiaire/planning";
}

@PostMapping("/planning/ajouter")
public String ajouterEvenementPlanning(@AuthenticationPrincipal CustomUserDetails userDetails,
                                        @RequestParam String motif,
                                        @RequestParam String date,
                                        @RequestParam String typeCouleur) {
    getStage(userDetails).ifPresent(stage -> {
        EvenementPersonnel evt = new EvenementPersonnel(stage, motif, LocalDate.parse(date), typeCouleur);
        evenementPersonnelRepository.save(evt);
    });
    return "redirect:/stagiaire/planning?succes=Evenement ajoute avec succes.";
}

@GetMapping("/messages")
public String afficherMessages(@AuthenticationPrincipal CustomUserDetails userDetails,
                               @RequestParam(required = false) Integer convId, Model model) {
    model.addAttribute("activePage", "messages");
    model.addAttribute("prenom", userDetails.getUtilisateur().getPrenom());
    model.addAttribute("nomComplet", userDetails.getUtilisateur().getPrenom() + " " + userDetails.getUtilisateur().getNom());
    model.addAttribute("initiales", userDetails.getUtilisateur().getPrenom().substring(0,1).toUpperCase()
            + userDetails.getUtilisateur().getNom().substring(0,1).toUpperCase());
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
    // Contacts disponibles (tous les utilisateurs sauf le stagiaire)
    List<Utilisateur> contacts = utilisateurRepository.findAll();
    contacts.remove(currentUser);
    model.addAttribute("contacts", contacts);
    return "stagiaire/messages";
}

@PostMapping("/messages/nouveau")
public String nouvelleConversation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam Integer destinataireId,
                                   @RequestParam String message) {
    Utilisateur currentUser = userDetails.getUtilisateur();
    Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElse(null);
    if (destinataire == null) return "redirect:/stagiaire/messages";
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
            return "redirect:/stagiaire/messages?convId=" + c.getId();
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
    return "redirect:/stagiaire/messages?convId=" + conv.getId();
}

@PostMapping("/messages/envoyer")
public String envoyerMessage(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @RequestParam Integer convId,
                             @RequestParam String contenu) {
    Conversation conv = conversationRepository.findById(convId).orElse(null);
    if (conv == null) return "redirect:/stagiaire/messages";
    Message msg = new Message();
    msg.setConversation(conv);
    msg.setExpediteur(userDetails.getUtilisateur());
    msg.setContenu(contenu);
    msg.setDateEnvoi(java.time.LocalDateTime.now());
    msg.setLu(false);
    conv.setDernierMessage(java.time.LocalDateTime.now());
    conversationRepository.save(conv);
    messageRepository.save(msg);
    return "redirect:/stagiaire/messages?convId=" + convId;
}
@PostMapping("/profil/modifier")
public String modifierProfil(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam String prenom,
                              @RequestParam String nom,
                              @RequestParam String email,
                              @RequestParam(required = false) String telephone,
                              @RequestParam(required = false) String adresse) {
    Utilisateur u = userDetails.getUtilisateur();
    u.setPrenom(prenom);
    u.setNom(nom);
    u.setEmail(email);
    u.setTelephone(telephone);
    u.setAdresse(adresse);
    utilisateurRepository.save(u);
    return "redirect:/stagiaire/profil?succes=Profil modifie avec succes.";
}

@PostMapping("/profil/mot-de-passe")
public String modifierMotDePasse(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  @RequestParam String ancienMotDePasse,
                                  @RequestParam String nouveauMotDePasse,
                                  @RequestParam String confirmerMotDePasse) {
    Utilisateur u = userDetails.getUtilisateur();
    if (!passwordEncoder.matches(ancienMotDePasse, u.getMotDePasse())) {
        return "redirect:/stagiaire/profil?erreur=Ancien mot de passe incorrect.";
    }
    if (!nouveauMotDePasse.equals(confirmerMotDePasse)) {
        return "redirect:/stagiaire/profil?erreur=Les nouveaux mots de passe ne correspondent pas.";
    }
    u.setMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
    utilisateurRepository.save(u);
    return "redirect:/stagiaire/profil?succes=Mot de passe modifie avec succes.";
}

    @PostMapping("/taches/{id}/statut")
    public String changerStatutTache(@PathVariable Integer id, @RequestParam String statut) {
        tacheRepository.findById(id).ifPresent(tache -> {
            tache.setStatut(Tache.StatutTache.valueOf(statut));
            tacheRepository.save(tache);
        });
        return "redirect:/stagiaire/taches";
    }

    @PostMapping("/taches/ajouter")
public String ajouterTache(@AuthenticationPrincipal CustomUserDetails userDetails,
                            @RequestParam String titre,
                            @RequestParam(required = false) String description,
                            @RequestParam String dateLimite,
                            @RequestParam String priorite) {
    getStage(userDetails).ifPresent(stage -> {
        Tache tache = new Tache();
        tache.setTitre(titre);
        tache.setDescription(description);
        tache.setDateLimite(LocalDate.parse(dateLimite));
        tache.setStatut(Tache.StatutTache.a_faire);
        tache.setStage(stage);
        tacheRepository.save(tache);
    });
    return "redirect:/stagiaire/taches?succes=Tache cree avec succes.";
}
}