package com.gestionstages.gestion_stages.services;

import com.gestionstages.gestion_stages.entities.Permission;
import com.gestionstages.gestion_stages.entities.PermissionMapping;
import com.gestionstages.gestion_stages.entities.Role;
import com.gestionstages.gestion_stages.repositories.PermissionMappingRepository;
import com.gestionstages.gestion_stages.repositories.PermissionRepository;
import com.gestionstages.gestion_stages.repositories.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionService {

    private final PermissionMappingRepository permissionMappingRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionService(PermissionMappingRepository permissionMappingRepository,
                            PermissionRepository permissionRepository,
                            RoleRepository roleRepository) {
        this.permissionMappingRepository = permissionMappingRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Initialise les mappings par défaut entre permissions et pages
     * Cette méthode crée les correspondances standards pour éviter les duplications
     */
    @Transactional
    public void initialiserMappingsParDefaut() {
        if (permissionMappingRepository.count() > 0) {
            return; // Déjà initialisé
        }

        // Mappings pour ENCADREUR
        creerMapping("Gérer stagiaires", "/encadreur/mes-stagiaires", "Accès à la gestion des stagiaires assignés");
        creerMapping("Consulter rapports", "/encadreur/livrables", "Accès aux livrables et rapports des stagiaires");
        creerMapping("Évaluer stagiaires", "/encadreur/evaluations", "Accès aux évaluations des stagiaires");
        creerMapping("Suivi progression", "/encadreur/suivi", "Accès au suivi de progression des stagiaires");

        // Mappings pour RESPONSABLE_STAGE
        creerMapping("Gérer stages", "/responsable/stages", "Accès à la gestion globale des stages");
        creerMapping("Affecter stagiaires", "/responsable/affectations", "Accès à l'affectation des stagiaires aux encadreurs");
        creerMapping("Valider demandes", "/responsable/demandes", "Accès à la validation des demandes de stage");
        creerMapping("Gérer encadreurs", "/responsable/encadreurs", "Accès à la gestion des encadreurs");

        // Mappings pour STAGIAIRE
        creerMapping("Voir mon stage", "/stagiaire/mon-stage", "Accès aux informations de son stage");
        creerMapping("Déposer livrables", "/stagiaire/livrables", "Accès au dépôt des livrables");
        creerMapping("Journal de bord", "/stagiaire/journal", "Accès au journal de bord");
        creerMapping("Voir évaluations", "/stagiaire/evaluations", "Accès à ses évaluations");

        // Mappings pour ADMINISTRATEUR
        creerMapping("Gérer utilisateurs", "/admin/utilisateurs", "Accès à la gestion des utilisateurs");
        creerMapping("Gérer rôles", "/admin/roles-permissions", "Accès à la gestion des rôles et permissions");
        creerMapping("Gérer services", "/admin/services", "Accès à la gestion des services");
        creerMapping("Gérer documents", "/admin/documents", "Accès à la gestion des documents");
        creerMapping("Dashboard", "/admin/dashboard", "Accès au tableau de bord administrateur");
    }

    private void creerMapping(String permissionNom, String cheminPage, String description) {
        if (!permissionMappingRepository.existsByPermissionNom(permissionNom) &&
            !permissionMappingRepository.existsByCheminPage(cheminPage)) {
            PermissionMapping mapping = new PermissionMapping(permissionNom, cheminPage, description);
            permissionMappingRepository.save(mapping);
        }
    }

    /**
     * Ajoute une permission à un rôle avec déduplication intelligente
     * Si une permission similaire existe déjà pour la même page, elle n'est pas dupliquée
     */
    @Transactional
    public void ajouterPermissionARole(String permissionNom, Role role) {
        // Vérifier si la permission existe déjà dans le rôle
        boolean permissionExiste = role.getPermissions().stream()
                .anyMatch(p -> p.getNom().equalsIgnoreCase(permissionNom));

        if (permissionExiste) {
            return; // Déjà présente
        }

        // Chercher un mapping existant pour cette permission
        Optional<PermissionMapping> mappingOpt = permissionMappingRepository.findByPermissionNom(permissionNom);

        if (mappingOpt.isPresent()) {
            PermissionMapping mapping = mappingOpt.get();
            
            // Vérifier s'il y a déjà une permission pour la même page (via les aliases)
            Set<String> pagesDejaMappees = new HashSet<>();
            pagesDejaMappees.add(mapping.getCheminPage());
            
            // Ajouter les pages des aliases
            for (PermissionMapping alias : mapping.getAliases()) {
                pagesDejaMappees.add(alias.getCheminPage());
            }

            // Vérifier si le rôle a déjà une permission pour une de ces pages
            boolean pageDejaPresente = role.getPermissions().stream()
                    .anyMatch(p -> {
                        Optional<PermissionMapping> pMapping = permissionMappingRepository.findByPermissionNom(p.getNom());
                        return pMapping.map(pm -> pagesDejaMappees.contains(pm.getCheminPage())).orElse(false);
                    });

            if (pageDejaPresente) {
                return; // Page déjà couverte par une autre permission
            }
        }

        // Créer ou récupérer la permission
        Permission permission = permissionRepository.findByNom(permissionNom)
                .orElseGet(() -> {
                    String code = normaliserCode(permissionNom);
                    return permissionRepository.save(new Permission(code, permissionNom, "Permission générée automatiquement"));
                });

        // Ajouter la permission au rôle
        role.getPermissions().add(permission);
        roleRepository.save(role);
    }

    /**
     * Retourne la liste des pages accessibles pour un rôle donné
     * avec déduplication automatique
     */
    public Set<String> getPagesAccessibles(Role role) {
        Set<String> pages = new HashSet<>();
        
        for (Permission permission : role.getPermissions()) {
            Optional<PermissionMapping> mappingOpt = permissionMappingRepository.findByPermissionNom(permission.getNom());
            if (mappingOpt.isPresent()) {
                PermissionMapping mapping = mappingOpt.get();
                pages.add(mapping.getCheminPage());
                
                // Ajouter les pages des aliases
                for (PermissionMapping alias : mapping.getAliases()) {
                    pages.add(alias.getCheminPage());
                }
            }
        }
        
        return pages;
    }

    /**
     * Crée un alias entre deux permissions (pour éviter les duplications)
     */
    @Transactional
    public void creerAlias(String permissionNom1, String permissionNom2) {
        PermissionMapping mapping1 = permissionMappingRepository.findByPermissionNom(permissionNom1)
                .orElseThrow(() -> new IllegalArgumentException("Permission non trouvée: " + permissionNom1));
        PermissionMapping mapping2 = permissionMappingRepository.findByPermissionNom(permissionNom2)
                .orElseThrow(() -> new IllegalArgumentException("Permission non trouvée: " + permissionNom2));

        mapping1.addAlias(mapping2);
        mapping2.addAlias(mapping1);
        permissionMappingRepository.save(mapping1);
        permissionMappingRepository.save(mapping2);
    }

    /**
     * Synchronise les permissions d'un rôle avec les mappings actifs
     * Met à jour le rôle pour refléter les changements de mapping
     */
    @Transactional
    public void synchroniserRoleAvecMappings(Role role) {
        Set<Permission> permissionsAMettreAJour = new HashSet<>();
        
        for (Permission permission : role.getPermissions()) {
            Optional<PermissionMapping> mappingOpt = permissionMappingRepository.findByPermissionNom(permission.getNom());
            if (mappingOpt.isPresent()) {
                PermissionMapping mapping = mappingOpt.get();
                
                // Vérifier les aliases et ajouter les permissions correspondantes
                for (PermissionMapping alias : mapping.getAliases()) {
                    permissionRepository.findByNom(alias.getPermissionNom())
                            .ifPresent(permissionsAMettreAJour::add);
                }
            }
        }
        
        role.getPermissions().addAll(permissionsAMettreAJour);
        roleRepository.save(role);
    }

    /**
     * Normalise un nom de permission en code technique
     */
    private String normaliserCode(String nom) {
        return nom.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Vérifie si une permission est en conflit avec une autre (même page)
     */
    public boolean estEnConflit(String permissionNom1, String permissionNom2) {
        Optional<PermissionMapping> mapping1 = permissionMappingRepository.findByPermissionNom(permissionNom1);
        Optional<PermissionMapping> mapping2 = permissionMappingRepository.findByPermissionNom(permissionNom2);

        if (mapping1.isEmpty() || mapping2.isEmpty()) {
            return false;
        }

        return mapping1.get().getCheminPage().equals(mapping2.get().getCheminPage());
    }
}
