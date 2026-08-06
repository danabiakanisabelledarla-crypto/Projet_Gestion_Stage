package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Document;
import com.gestionstages.gestion_stages.entities.Livrable;
import com.gestionstages.gestion_stages.repositories.DocumentRepository;
import com.gestionstages.gestion_stages.repositories.LivrableRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final LivrableRepository livrableRepository;

    public DocumentController(DocumentRepository documentRepository,
                              LivrableRepository livrableRepository) {
        this.documentRepository = documentRepository;
        this.livrableRepository = livrableRepository;
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Integer id) throws IOException {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        return construireReponseFichier(
                resoudreCheminFichier(doc.getCheminFichier()),
                doc.getNomFichier(),
                "attachment"
        );
    }

    @GetMapping("/documents/{id}/preview")
    public ResponseEntity<Resource> previsualiserDocument(@PathVariable Integer id) throws IOException {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return construireReponseFichier(
                resoudreCheminFichier(doc.getCheminFichier()),
                doc.getNomFichier(),
                "inline"
        );
    }

    @GetMapping("/documents/livrables/{id}")
    public ResponseEntity<Resource> telechargerLivrable(@PathVariable Integer id) throws IOException {
        Livrable livrable = livrableRepository.findById(id).orElse(null);
        if (livrable == null) {
            return ResponseEntity.notFound().build();
        }
        Path chemin = resoudreCheminFichier(livrable.getFichier());
        String nom = chemin.getFileName() != null ? chemin.getFileName().toString() : livrable.getTitre();
        return construireReponseFichier(chemin, nom, "attachment");
    }

    @GetMapping("/documents/livrables/{id}/preview")
    public ResponseEntity<Resource> previsualiserLivrable(@PathVariable Integer id) throws IOException {
        Livrable livrable = livrableRepository.findById(id).orElse(null);
        if (livrable == null) {
            return ResponseEntity.notFound().build();
        }
        Path chemin = resoudreCheminFichier(livrable.getFichier());
        String nom = chemin.getFileName() != null ? chemin.getFileName().toString() : livrable.getTitre();
        return construireReponseFichier(chemin, nom, "inline");
    }

    private ResponseEntity<Resource> construireReponseFichier(Path chemin,
                                                               String nomFichier,
                                                               String disposition) throws IOException {
        if (!Files.exists(chemin)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(chemin);
        String contentType = Files.probeContentType(chemin);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + nomFichier.replace("\"", "") + "\"")
                .body(resource);
    }

    private Path resoudreCheminFichier(String cheminEnregistre) {
        if (cheminEnregistre == null || cheminEnregistre.isBlank()) {
            return Paths.get("");
        }

        String normalizedPath = cheminEnregistre.replace('\\', '/').trim();
        Path chemin = Paths.get(normalizedPath).normalize();
        if (chemin.isAbsolute() && Files.exists(chemin)) {
            return chemin;
        }

        Path dossierCourant = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> bases = new ArrayList<>();
        bases.add(dossierCourant);
        if (dossierCourant.getParent() != null) {
            bases.add(dossierCourant.getParent());
            bases.add(dossierCourant.getParent().resolve("gestion-stages"));
        }
        bases.add(dossierCourant.resolve("gestion-stages"));
        bases.add(dossierCourant.resolve("target"));

        Path candidate = chercherCheminDansBases(bases, chemin);
        if (candidate != null) {
            return candidate;
        }

        Path nomFichier = chemin.getFileName();
        if (nomFichier != null) {
            List<Path> fallbackBases = new ArrayList<>();
            fallbackBases.add(dossierCourant);
            fallbackBases.add(dossierCourant.resolve("uploads"));
            fallbackBases.add(dossierCourant.resolve("uploads").resolve("documents"));
            fallbackBases.add(dossierCourant.resolve("gestion-stages"));
            fallbackBases.add(dossierCourant.resolve("gestion-stages").resolve("uploads"));
            fallbackBases.add(dossierCourant.resolve("gestion-stages").resolve("uploads").resolve("documents"));
            if (dossierCourant.getParent() != null) {
                fallbackBases.add(dossierCourant.getParent().resolve("uploads"));
                fallbackBases.add(dossierCourant.getParent().resolve("uploads").resolve("documents"));
            }
            for (Path base : fallbackBases) {
                if (base == null) continue;
                Path fallback = base.resolve(nomFichier).normalize();
                if (Files.exists(fallback)) {
                    return fallback;
                }
            }
        }

        return chemin;
    }

    private Path chercherCheminDansBases(List<Path> bases, Path chemin) {
        for (Path base : bases) {
            if (base == null) continue;
            try {
                Path candidate = base.resolve(chemin).normalize();
                if (Files.exists(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
