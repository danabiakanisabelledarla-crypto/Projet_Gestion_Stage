package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.EmailService;
import com.gestionstages.gestion_stages.entities.DemandeStage;
import com.gestionstages.gestion_stages.entities.Document;
import com.gestionstages.gestion_stages.repositories.DemandeStageRepository;
import com.gestionstages.gestion_stages.repositories.DocumentRepository;
import com.gestionstages.gestion_stages.repositories.UtilisateurRepository;
import com.gestionstages.gestion_stages.repositories.StagiaireRepository;
import com.gestionstages.gestion_stages.repositories.StageRepository;
import com.gestionstages.gestion_stages.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/candidat")
public class CandidatController {

    private final DemandeStageRepository demandeStageRepository;
    private final DocumentRepository documentRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final StagiaireRepository stagiaireRepository;
    private final StageRepository stageRepository;
    private final EmailService emailService;

    private static final String DOSSIER_UPLOAD = "uploads/";

    public CandidatController(DemandeStageRepository demandeStageRepository,
                               DocumentRepository documentRepository,
                               UtilisateurRepository utilisateurRepository,
                               StagiaireRepository stagiaireRepository,
                               StageRepository stageRepository,
                               EmailService emailService) {
        this.demandeStageRepository = demandeStageRepository;
        this.documentRepository = documentRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.stagiaireRepository = stagiaireRepository;
        this.stageRepository = stageRepository;
        this.emailService = emailService;
    }

    //@GetMapping("/")
    //public String afficherAccueil() {
     //   return "accueil";
    //}

    @GetMapping("/demande")
    public String afficherFormulaire(@RequestParam(required = false) Boolean succes,
                                     Model model) {
        model.addAttribute("succes", Boolean.TRUE.equals(succes));
        return "candidat/demande";
    }

    @PostMapping("/demande")
public String soumettreFormulaire(
        @RequestParam String nom,
        @RequestParam String prenom,
        @RequestParam String email,
        @RequestParam String ecole,
        @RequestParam String filiere,
        @RequestParam(required = false) String filiereAutre,
        @RequestParam String niveau,
        @RequestParam String dureeSouhaitee,
        @RequestParam(required = false) String dureeAutre,
        @RequestParam(required = false) String telephone,
        @RequestParam(required = false) String dateNaissance,
        @RequestParam(required = false) String ville,
        @RequestParam(required = false) String villeAutre,
        @RequestParam(required = false) String genre,
        @RequestParam(required = false) String anneeAcademique,
        @RequestParam(required = false) String domaineInteret,
        @RequestParam(required = false) String domaineAutre,
        @RequestParam(required = false) String motivation,
        @RequestParam(required = false) MultipartFile cni,
        @RequestParam(required = false) MultipartFile lettreStage,
        @RequestParam(required = false) MultipartFile cv,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        RedirectAttributes redirectAttributes) {

    String filiereFinale = "Autre".equalsIgnoreCase(filiere) && filiereAutre != null && !filiereAutre.isBlank()
            ? filiereAutre.trim() : filiere;
    String dureeFinale = "Autre".equalsIgnoreCase(dureeSouhaitee) && dureeAutre != null && !dureeAutre.isBlank()
            ? dureeAutre.trim() : dureeSouhaitee;
    String domaineFinal = "Autre".equalsIgnoreCase(domaineInteret) && domaineAutre != null && !domaineAutre.isBlank()
            ? domaineAutre.trim() : domaineInteret;
    DemandeStage demande = new DemandeStage(nom, prenom, ecole,
            filiereFinale, niveau, dureeFinale);
    demande.setEmail(email.trim().toLowerCase());
    Optional<com.gestionstages.gestion_stages.entities.Utilisateur> compte =
            utilisateurRepository.findByEmail(demande.getEmail());
    if (compte.isPresent()) {
        Optional<com.gestionstages.gestion_stages.entities.Stagiaire> ancien =
                stagiaireRepository.findByUtilisateurId(compte.get().getId());
        if (ancien.isPresent()) {
            var ancienStage = stageRepository.findByStagiaireId(ancien.get().getId()).orElse(null);
            if (ancienStage != null && ancienStage.getDateFin() != null
                    && !ancienStage.getDateFin().isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("erreur",
                        "Impossible de déposer votre demande : vous avez déjà un compte actif.");
                return "redirect:/candidat/demande";
            }
        }
    }
    demande.setTelephone(telephone);
    demande.setDateNaissance(dateNaissance == null || dateNaissance.isBlank()
            ? null : LocalDate.parse(dateNaissance));
    demande.setVille("Autre".equalsIgnoreCase(ville) && villeAutre != null && !villeAutre.isBlank()
            ? villeAutre.trim() : ville);
    demande.setGenre(genre);
    demande.setAnneeAcademique(anneeAcademique);
    demande.setDomaineInteret(domaineFinal);
    demande.setMotivation(motivation);
    demandeStageRepository.save(demande);

    if (userDetails != null) {
        var utilisateur = userDetails.getUtilisateur();
        if (utilisateur.getEmail().equalsIgnoreCase(demande.getEmail())) {
            utilisateur.setNom(nom.trim());
            utilisateur.setPrenom(prenom.trim());
            utilisateur.setTelephone(telephone);
            utilisateur.setDateNaissance(demande.getDateNaissance());
            utilisateur.setVille(demande.getVille());
            utilisateurRepository.save(utilisateur);
        }
    }

    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));

        sauvegarderDocument(cni, "CNI", demande);
        sauvegarderDocument(lettreStage, "LETTRE_STAGE", demande);
        sauvegarderDocument(cv, "CV", demande);

    } catch (IOException e) {
        System.err.println("Erreur upload : " + e.getMessage());
    }

    boolean emailEnvoye = emailService.envoyerAccuseReceptionCandidature(
            demande.getEmail(),
            demande.getPrenom() + " " + demande.getNom());
    if (!emailEnvoye) {
        System.err.println("L'accusé de réception n'a pas pu être envoyé à " + demande.getEmail());
    }

    return "redirect:/candidat/demande?succes=true";
}

    private void sauvegarderDocument(MultipartFile fichier, String typeDocument,
                                      DemandeStage demande) throws IOException {
        if (fichier != null && !fichier.isEmpty()) {
            String nomFichier = typeDocument.toLowerCase() + "_"
                    + demande.getId() + "_" + fichier.getOriginalFilename();
            Path chemin = Paths.get(DOSSIER_UPLOAD + nomFichier);
            Files.write(chemin, fichier.getBytes());

            Document doc = new Document(fichier.getOriginalFilename(),
                    typeDocument, chemin.toString());
            doc.setDemandeStage(demande);
            doc.setTailleOctets(fichier.getSize());
            documentRepository.save(doc);
        }
    }
    @GetMapping("/suivi")
        public String afficherSuivi() {
            return "candidat/suivi";
        }

        @PostMapping("/suivi")
        public String rechercherDemande(@RequestParam String email, Model model) {
            demandeStageRepository.findAll().stream()
                    .filter(d -> (d.getEmail() != null && d.getEmail().equalsIgnoreCase(email.trim()))
                            || (d.getCommentaire() != null
                            && d.getCommentaire().equalsIgnoreCase("Email candidat : " + email.trim())))
                    .findFirst()
                    .ifPresentOrElse(
                            demande -> model.addAttribute("demande", demande),
                            () -> model.addAttribute("erreur",
                                    "Aucune demande trouvee pour cet email. "
                                + "Verifiez l'email utilise lors de votre candidature.")
                    );
            return "candidat/suivi";
        }
}
