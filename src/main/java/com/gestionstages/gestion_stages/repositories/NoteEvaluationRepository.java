package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.NoteEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteEvaluationRepository extends JpaRepository<NoteEvaluation, Integer> {

    List<NoteEvaluation> findByEvaluationId(Integer evaluationId);
}