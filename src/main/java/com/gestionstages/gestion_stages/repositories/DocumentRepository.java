package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Integer> {

    List<Document> findByDemandeStageId(Integer demandeStageId);

    List<Document> findByStageId(Integer stageId);
}
