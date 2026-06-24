package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.JournalBord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JournalBordRepository extends JpaRepository<JournalBord, Integer> {

    List<JournalBord> findByStageIdOrderByDateActiviteDesc(Integer stageId);
}