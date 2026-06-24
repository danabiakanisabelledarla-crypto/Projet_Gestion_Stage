package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Integer> {

    List<Tache> findByStageId(Integer stageId);

    List<Tache> findByStageIdAndStatut(Integer stageId, Tache.StatutTache statut);
}