package com.gestionstages.gestion_stages.controllers;

import com.gestionstages.gestion_stages.entities.Document;
import com.gestionstages.gestion_stages.repositories.DocumentRepository;
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

@Controller
public class DocumentController {

    private final DocumentRepository documentRepository;

    public DocumentController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Integer id) throws IOException {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Path chemin = Paths.get(doc.getCheminFichier());
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
                        "attachment; filename=\"" + doc.getNomFichier() + "\"")
                .body(resource);
    }
}
