package com.gestionstages.gestion_stages.services;

import com.gestionstages.gestion_stages.entities.ApplicationSetting;
import com.gestionstages.gestion_stages.repositories.ApplicationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ApplicationSettingService {

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("platformName", "Gestion des Stages"),
            Map.entry("companyName", "Digital Transformation Alliance"),
            Map.entry("contactEmail", "contact@dta-alliance.com"),
            Map.entry("phone", "+237 6XX XXX XXX"),
            Map.entry("address", "Yaounde, Cameroun"),
            Map.entry("timezone", "Africa/Douala"),
            Map.entry("academicYear", "2026 - 2027"),
            Map.entry("startDate", "2026-09-01"),
            Map.entry("maxDuration", "6"),
            Map.entry("language", "fr"),
            Map.entry("emailNotifications", "true"),
            Map.entry("appNotifications", "true"),
            Map.entry("maxFileSize", "25"),
            Map.entry("storage", "local"),
            Map.entry("dateFormat", "dd/MM/yyyy"),
            Map.entry("maintenanceMode", "false")
    );

    private final ApplicationSettingRepository repository;

    public ApplicationSettingService(ApplicationSettingRepository repository) {
        this.repository = repository;
    }

    public Map<String, String> getAll() {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        repository.findAll().forEach(setting -> result.put(setting.getKey(), setting.getValue()));
        return result;
    }

    @Transactional
    public void save(Map<String, String> values) {
        values.forEach((key, value) ->
                repository.save(new ApplicationSetting(key, value == null ? "" : value.trim())));
    }

    @Transactional
    public void reset() {
        repository.deleteAll();
    }
}
