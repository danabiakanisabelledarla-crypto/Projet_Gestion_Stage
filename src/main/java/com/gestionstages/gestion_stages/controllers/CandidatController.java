package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.DemandeStage;
import com.gestionstages.gestion_stages.entities.Document;
import com.gestionstages.gestion_stages.repositories.DemandeStageRepository;
import com.gestionstages.gestion_stages.repositories.DocumentRepository;
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

@Controller
@RequestMapping("/candidat")
public class CandidatController {

    private final DemandeStageRepository demandeStageRepository;
    private final DocumentRepository documentRepository;

    private static final String DOSSIER_UPLOAD = "uploads/";

    public CandidatController(DemandeStageRepository demandeStageRepository,
                               DocumentRepository documentRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.documentRepository = documentRepository;
    }

    //@GetMapping("/")
    //public String afficherAccueil() {
     //   return "accueil";
    //}

    @GetMapping("/demande")
    public String afficherFormulaire() {
        return "candidat/demande";
    }

    @PostMapping("/demande")
public String soumettreFormulaire(
        @RequestParam String nom,
        @RequestParam String prenom,
        @RequestParam String email,
        @RequestParam String ecole,
        @RequestParam String filiere,
        @RequestParam String niveau,
        @RequestParam String dureeSouhaitee,
        @RequestParam(required = false) MultipartFile cni,
        @RequestParam(required = false) MultipartFile lettreStage,
        @RequestParam(required = false) MultipartFile cv,
        RedirectAttributes redirectAttributes) {

    DemandeStage demande = new DemandeStage(nom, prenom, ecole,
            filiere, niveau, dureeSouhaitee);
    demande.setCommentaire("Email candidat : " + email);
    demandeStageRepository.save(demande);

    try {
        Files.createDirectories(Paths.get(DOSSIER_UPLOAD));

        sauvegarderDocument(cni, "CNI", demande);
        sauvegarderDocument(lettreStage, "LETTRE_STAGE", demande);
        sauvegarderDocument(cv, "CV", demande);

    } catch (IOException e) {
        System.err.println("Erreur upload : " + e.getMessage());
    }

    redirectAttributes.addFlashAttribute("messageSucces",
            "Demande envoyée avec succès !");
    return "redirect:/";
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
            documentRepository.save(doc);
        }
    }
    @GetMapping("/suivi")
        public String afficherSuivi() {
            return "candidat/suivi";
        }

        @PostMapping("/suivi")
        public String rechercherDemande(@RequestParam String email, Model model) {
            // On cherche la demande par l'email stocké dans le commentaire
            demandeStageRepository.findAll().stream()
                    .filter(d -> d.getCommentaire() != null
                            && d.getCommentaire().equals("Email candidat : " + email))
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