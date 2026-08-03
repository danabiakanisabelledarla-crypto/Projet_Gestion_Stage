package com.gestionstages.gestion_stages.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**",
                        "/login", "/mot-de-passe-oublie", "/reinitialiser-mot-de-passe/**",
                        "/candidat/**", "/", "/a-propos", "/contact",
                        "/faq", "/mentions-legales", "/confidentialite", "/error").permitAll()
                .requestMatchers("/profil/**", "/documents/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMINISTRATEUR")

                .requestMatchers("/encadreur/dashboard")
                    .access(espaceEtPermission("ENCADREUR", null))
                .requestMatchers("/encadreur/mes-stagiaires/**",
                        "/encadreur/objectifs/**",
                        "/encadreur/taches/**",
                        "/encadreur/evaluations/**")
                    .access(espaceEtPermission("ENCADREUR", "GERER_STAGIAIRES"))
                .requestMatchers("/encadreur/livrables/valider/**",
                        "/encadreur/livrables/rejeter/**",
                        "/encadreur/livrables/commenter")
                    .access(espaceEtPermission("ENCADREUR", "VALIDER_DOCUMENTS"))
                .requestMatchers("/encadreur/livrables/**")
                    .access(espaceEtPermission("ENCADREUR", "CONSULTER_RAPPORTS"))
                .requestMatchers("/encadreur/profil/**")
                    .access(espaceEtPermission("ENCADREUR", null))
                .requestMatchers("/encadreur/planning/**")
                    .access(espaceEtUnePermission("ENCADREUR", "CONSULTER_PLANNING", "GERER_PLANNING"))
                .requestMatchers("/encadreur/messagerie/**", "/encadreur/notifications")
                    .access(espaceEtUnePermission("ENCADREUR", "ENVOYER_NOTIFICATIONS", "GERER_NOTIFICATIONS"))
                .requestMatchers("/encadreur/**").denyAll()

                .requestMatchers("/responsable/dashboard")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", null))
                .requestMatchers("/responsable/profil/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", null))
                .requestMatchers("/responsable/demandes/**", "/responsable/admissions/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", "GERER_DEMANDES_STAGE"))
                .requestMatchers("/responsable/cloture/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", "CLOTURER_STAGES"))
                .requestMatchers("/responsable/affectations/**")
                    .access(espaceEtUnePermission("RESPONSABLE_STAGE", "GERER_AFFECTATIONS", "AFFECTER_STAGIAIRES"))
                .requestMatchers("/responsable/stagiaires/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", "GERER_STAGIAIRES"))
                .requestMatchers("/responsable/suivi/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", "GERER_STAGIAIRES"))
                .requestMatchers("/responsable/dossiers/**", "/responsable/archives/**")
                    .access(espaceEtPermission("RESPONSABLE_STAGE", "GERER_DOSSIERS"))
                .requestMatchers("/responsable/planning/**")
                    .access(espaceEtUnePermission("RESPONSABLE_STAGE", "CONSULTER_PLANNING", "GERER_PLANNING"))
                .requestMatchers("/responsable/notifications/**")
                    .access(espaceEtUnePermission("RESPONSABLE_STAGE", "ENVOYER_NOTIFICATIONS", "GERER_NOTIFICATIONS"))
                .requestMatchers("/responsable/**").denyAll()

                .requestMatchers("/stagiaire/**")
                    .access(espaceEtPermission("STAGIAIRE", null))
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/redirection", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .tokenValiditySeconds(14 * 24 * 60 * 60)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    private WebExpressionAuthorizationManager espaceEtPermission(String espace, String permission) {
        String expressionEspace = "(hasRole('" + espace + "') or hasAuthority('ESPACE_" + espace + "'))";
        String expression = permission == null
                ? expressionEspace
                : expressionEspace + " and hasAuthority('PERM_" + permission + "')";
        return new WebExpressionAuthorizationManager(expression);
    }

    private WebExpressionAuthorizationManager espaceEtUnePermission(String espace,
                                                                      String premiere,
                                                                      String seconde) {
        String expression = "(hasRole('" + espace + "') or hasAuthority('ESPACE_" + espace + "'))"
                + " and (hasAuthority('PERM_" + premiere + "')"
                + " or hasAuthority('PERM_" + seconde + "'))";
        return new WebExpressionAuthorizationManager(expression);
    }
}
