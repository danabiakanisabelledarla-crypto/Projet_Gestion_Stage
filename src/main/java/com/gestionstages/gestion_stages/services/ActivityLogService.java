package com.gestionstages.gestion_stages.services;

import com.gestionstages.gestion_stages.entities.ActivityLog;
import com.gestionstages.gestion_stages.repositories.ActivityLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    public void log(String action, String details, String utilisateurNom) {
        activityLogRepository.save(new ActivityLog(action, details, utilisateurNom));
    }
}
