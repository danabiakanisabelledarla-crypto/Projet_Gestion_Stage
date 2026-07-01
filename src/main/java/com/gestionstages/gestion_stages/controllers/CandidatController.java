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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/candidat")
public class CandidatController {

    private final DemandeStageRepository demandeStageRepository;
    private final DocumentRepository documentRepository;

    // Dossier où les fichiers seront stockés
    private static final String DOSSIER_UPLOAD = "uploads/";

    public CandidatController(DemandeStageRepository demandeStageRepository,
                               DocumentRepository documentRepository) {
        this.demandeStageRepository = demandeStageRepository;
        this.documentRepository = documentRepository;
    }

    @GetMapping("/demande")
    public String afficherFormulaire() {
        return "candidat/demande";
    }

    @PostMapping("/demande")
    public String soumettreFormulaire(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String ecole,
            @RequestParam String filiere,
            @RequestParam String niveau,
            @RequestParam String dureeSouhaitee,
            @RequestParam(required = false) MultipartFile cv,
            @RequestParam(required = false) MultipartFile lettreMotivation,
            Model model) {

        // 1. Sauvegarder la demande
        DemandeStage demande = new DemandeStage(nom, prenom, ecole, filiere, niveau, dureeSouhaitee);
        demandeStageRepository.save(demande);

        // 2. Sauvegarder les fichiers si fournis
        try {
            Files.createDirectories(Paths.get(DOSSIER_UPLOAD));

            if (cv != null && !cv.isEmpty()) {
                String nomFichierCv = "cv_" + demande.getId() + "_" + cv.getOriginalFilename();
                Path cheminCv = Paths.get(DOSSIER_UPLOAD + nomFichierCv);
                Files.write(cheminCv, cv.getBytes());

                Document docCv = new Document(cv.getOriginalFilename(), "CV", cheminCv.toString());
                docCv.setDemandeStage(demande);
                documentRepository.save(docCv);
            }

            if (lettreMotivation != null && !lettreMotivation.isEmpty()) {
                String nomFichierLm = "lm_" + demande.getId() + "_" + lettreMotivation.getOriginalFilename();
                Path cheminLm = Paths.get(DOSSIER_UPLOAD + nomFichierLm);
                Files.write(cheminLm, lettreMotivation.getBytes());

                Document docLm = new Document(lettreMotivation.getOriginalFilename(),
                        "LETTRE_MOTIVATION", cheminLm.toString());
                docLm.setDemandeStage(demande);
                documentRepository.save(docLm);
            }
        } catch (IOException e) {
            System.err.println("Erreur upload fichier : " + e.getMessage());
        }

        model.addAttribute("succes", true);
        return "candidat/demande";
    }
}