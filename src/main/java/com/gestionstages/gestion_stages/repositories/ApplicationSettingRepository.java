package com.gestionstages.gestion_stages.repositories;

import com.gestionstages.gestion_stages.entities.ApplicationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSettingRepository extends JpaRepository<ApplicationSetting, String> {
}
