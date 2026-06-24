package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    List<Evaluation> findByStageId(Integer stageId);
}