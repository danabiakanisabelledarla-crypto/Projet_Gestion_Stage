package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.DemandeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DemandeStageRepository extends JpaRepository<DemandeStage, Integer> {

    List<DemandeStage> findByStatut(DemandeStage.StatutDemande statut);
}