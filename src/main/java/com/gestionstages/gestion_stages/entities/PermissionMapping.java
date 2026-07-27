package com.gestionstages.gestion_stages.entities;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "permission_mappings")
public class PermissionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 120)
    private String permissionNom;

    @Column(nullable = false, length = 255)
    private String cheminPage;

    @Column(length = 100)
    private String description;

    @Column(nullable = false)
    private Boolean actif = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "mapping_aliases",
        joinColumns = @JoinColumn(name = "mapping_id"),
        inverseJoinColumns = @JoinColumn(name = "alias_id")
    )
    private Set<PermissionMapping> aliases = new LinkedHashSet<>();

    public PermissionMapping() {
    }

    public PermissionMapping(String permissionNom, String cheminPage, String description) {
        this.permissionNom = permissionNom;
        this.cheminPage = cheminPage;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPermissionNom() {
        return permissionNom;
    }

    public void setPermissionNom(String permissionNom) {
        this.permissionNom = permissionNom;
    }

    public String getCheminPage() {
        return cheminPage;
    }

    public void setCheminPage(String cheminPage) {
        this.cheminPage = cheminPage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Set<PermissionMapping> getAliases() {
        return aliases;
    }

    public void setAliases(Set<PermissionMapping> aliases) {
        this.aliases = aliases == null ? new LinkedHashSet<>() : aliases;
    }

    public void addAlias(PermissionMapping alias) {
        if (this.aliases == null) {
            this.aliases = new LinkedHashSet<>();
        }
        this.aliases.add(alias);
    }
}
