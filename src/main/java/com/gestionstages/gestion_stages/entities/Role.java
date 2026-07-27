package com.gestionstages.gestion_stages.entities;
import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "roles")

public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "espace", length = 40)
    private String espace;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @OrderBy("nom ASC")
    private Set<Permission> permissions = new LinkedHashSet<>();

    //CONTRUCTEUR   
    public Role(){

    }
    public Role(String libelle, String description){
        this.libelle = libelle ;
        this.description = description;
    }

    //Getters et Setters
    public Integer getId(){
        return id;
    }
    public void setId(Integer id){
        this.id = id;
    }

    public String getLibelle(){
        return libelle;
    }

    public void setLibelle(String libelle){
        this.libelle = libelle;
    }
    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }

    public String getEspace() {
        return espace;
    }

    public void setEspace(String espace) {
        this.espace = espace;
    }

    public String getEspaceEffectif() {
        if (espace != null && !espace.isBlank()) {
            return espace;
        }
        return switch (libelle) {
            case "ADMINISTRATEUR", "RESPONSABLE_STAGE", "ENCADREUR", "STAGIAIRE" -> libelle;
            default -> "STAGIAIRE";
        };
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions == null ? new LinkedHashSet<>() : permissions;
    }

    
}
