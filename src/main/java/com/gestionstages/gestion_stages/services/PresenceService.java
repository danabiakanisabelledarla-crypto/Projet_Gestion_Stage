package com.gestionstages.gestion_stages.services;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {

    private static final Duration ONLINE_WINDOW = Duration.ofSeconds(45);
    private final Map<Integer, Instant> lastSeen = new ConcurrentHashMap<>();

    public void markOnline(Integer userId) {
        if (userId != null) {
            lastSeen.put(userId, Instant.now());
        }
    }

    public boolean isOnline(Integer userId) {
        Instant activity = lastSeen.get(userId);
        return activity != null && activity.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }
}
