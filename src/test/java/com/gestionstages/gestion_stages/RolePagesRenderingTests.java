package com.gestionstages.gestion_stages;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rolepages;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RolePagesRenderingTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void publicPagesRenderForAnonymousVisitors() throws Exception {
        for (String route : new String[]{
                "/",
                "/a-propos",
                "/candidat/suivi",
                "/candidat/demande",
                "/contact",
                "/mot-de-passe-oublie"
        }) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void applicationTrackingPageRendersSearchAndResult() throws Exception {
        mockMvc.perform(get("/candidat/suivi"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/candidat/suivi")
                        .with(csrf())
                        .param("email", "stagiaire@gestion-stages.com"))
                .andExpect(status().isOk());
    }

    @Test
    void adminPagesRenderAfterLogin() throws Exception {
        MockHttpSession session = login("admin@gestion-stages.com", "admin1234");
        for (String route : new String[]{
                "/admin/dashboard",
                "/admin/stagiaires",
                "/admin/services",
                "/admin/documents",
                "/admin/rapports",
                "/admin/journal",
                "/admin/notifications",
                "/admin/messages"
        }) {
            int status = mockMvc.perform(get(route).session(session))
                    .andReturn().getResponse().getStatus();
            assertEquals(200, status, "Route Admin en échec : " + route);
        }
    }

    @Test
    void responsablePagesRenderAfterLogin() throws Exception {
        MockHttpSession session = login("responsable@gestion-stages.com", "resp1234");
        for (String route : new String[]{
                "/responsable/dashboard",
                "/responsable/demandes",
                "/responsable/stagiaires",
                "/responsable/dossiers",
                "/responsable/cloture",
                "/responsable/planning",
                "/responsable/profil",
                "/responsable/profil/mot-de-passe",
                "/responsable/messages"
        }) {
            int status = mockMvc.perform(get(route).session(session))
                    .andReturn().getResponse().getStatus();
            assertEquals(200, status, "Route Responsable en echec : " + route);
        }
    }

    @Test
    void encadreurPagesRenderAfterLogin() throws Exception {
        MockHttpSession session = login("encadreur@gestion-stages.com", "enc1234");
        for (String route : new String[]{
                "/encadreur/dashboard",
                "/encadreur/mes-stagiaires",
                "/encadreur/objectifs",
                "/encadreur/taches",
                "/encadreur/livrables",
                "/encadreur/evaluations",
                "/encadreur/planning",
                "/encadreur/profil",
                "/encadreur/messagerie",
                "/encadreur/notifications"
        }) {
            int status = mockMvc.perform(get(route).session(session))
                    .andReturn().getResponse().getStatus();
            assertEquals(200, status, "Route Encadreur en échec : " + route);
        }
    }

    @Test
    void stagiairePagesRenderAfterLogin() throws Exception {
        MockHttpSession session = login("stagiaire@gestion-stages.com", "stag1234");
        for (String route : new String[]{
                "/stagiaire/dashboard",
                "/stagiaire/objectifs",
                "/stagiaire/taches",
                "/stagiaire/livrables",
                "/stagiaire/planning",
                "/stagiaire/journal",
                "/stagiaire/rapport",
                "/stagiaire/profil",
                "/stagiaire/messages"
        }) {
            int status = mockMvc.perform(get(route).session(session))
                    .andReturn().getResponse().getStatus();
            assertEquals(200, status, "Route Stagiaire en échec : " + route);
        }
    }

    @Test
    void stagiairePreferencesArePersistedAndReturnedAsJson() throws Exception {
        MockHttpSession session = login("stagiaire@gestion-stages.com", "stag1234");

        mockMvc.perform(post("/stagiaire/preferences")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": false,
                                  "system": true,
                                  "tasks": false,
                                  "dark": true,
                                  "language": "en"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(false))
                .andExpect(jsonPath("$.system").value(true))
                .andExpect(jsonPath("$.tasks").value(false))
                .andExpect(jsonPath("$.dark").value(true))
                .andExpect(jsonPath("$.language").value("en"));

        mockMvc.perform(get("/stagiaire/preferences").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dark").value(true))
                .andExpect(jsonPath("$.language").value("en"));
    }

    @Test
    void stagiaireJournalAndObjectivesReflectNewDatabaseEntries() throws Exception {
        MockHttpSession session = login("stagiaire@gestion-stages.com", "stag1234");
        String journalText = "Synchronisation AJAX du journal";
        String objectiveText = "Objectif dynamique jeudi";

        mockMvc.perform(post("/stagiaire/journal/ajouter")
                        .session(session)
                        .with(csrf())
                        .param("dateActivite", java.time.LocalDate.now().toString())
                        .param("travauxRealises", journalText)
                        .param("duree", "6")
                        .param("difficultes", "")
                        .param("solutions", "Test automatique"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/stagiaire/journal").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(journalText)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("6 heure(s)")));

        mockMvc.perform(post("/stagiaire/objectifs/creer")
                        .session(session)
                        .with(csrf())
                        .param("libelle", objectiveText)
                        .param("description", "Objectif enregistré en base")
                        .param("priorite", "moyenne")
                        .param("dateLimite", "2026-08-20"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/stagiaire/objectifs").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(objectiveText)));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin()
                        .user(username)
                        .password(password))
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
