package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Integer> {
    List<ActivityLog> findTop50ByOrderByDateActiviteDesc();
}
