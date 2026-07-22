package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.EvenementPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvenementPersonnelRepository extends JpaRepository<EvenementPersonnel, Integer> {
    List<EvenementPersonnel> findByStageId(Integer stageId);
}
