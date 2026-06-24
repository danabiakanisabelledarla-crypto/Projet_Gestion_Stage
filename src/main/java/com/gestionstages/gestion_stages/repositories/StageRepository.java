package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Integer> {

    Optional<Stage> findByStagiaireId(Integer stagiaireId);

    List<Stage> findByEncadreurId(Integer encadreurId);

    List<Stage> findByStatut(Stage.StatutStage statut);
}