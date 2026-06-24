package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Objectif;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ObjectifRepository extends JpaRepository<Objectif, Integer> {

    List<Objectif> findByStageIdOrderByOrdreAsc(Integer stageId);
}