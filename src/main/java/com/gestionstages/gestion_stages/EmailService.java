package com.gestionstages.gestion_stages;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String adresseExpediteur;
    private final String urlPlateforme;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String adresseExpediteur,
                        @Value("${app.base-url:http://localhost:8080}") String urlPlateforme) {
        this.mailSender = mailSender;
        this.adresseExpediteur = adresseExpediteur == null ? "" : adresseExpediteur.trim();
        String url = urlPlateforme == null ? "" : urlPlateforme.trim();
        this.urlPlateforme = (url.isBlank() ? "http://localhost:8080" : url).replaceAll("/+$", "");
    }

    private String expediteur() {
        return adresseExpediteur.isBlank() ? "innotechlab26@gmail.com" : adresseExpediteur;
    }

    public boolean envoyerAccuseReceptionCandidature(String destinataire, String prenomNom) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = creerMessageDeMarque(
                    message,
                    destinataire,
                    "Nous avons bien reçu votre candidature");

            String contenu = enteteEmail()
                    + "<div style='padding:34px 36px;background:#ffffff;border:1px solid #e5e7eb;border-top:0'>"
                    + "<h2 style='margin:0 0 20px;color:#172033;font-size:23px'>Candidature bien reçue</h2>"
                    + "<p style='margin:0 0 14px;color:#374151;font-size:15px;line-height:1.75'>Bonjour <strong>"
                    + echapperHtml(prenomNom) + "</strong>,</p>"
                    + "<p style='margin:0 0 14px;color:#374151;font-size:15px;line-height:1.75'>"
                    + "Nous avons bien reçu votre candidature à la suite de votre demande de stage. "
                    + "Notre équipe va étudier votre dossier et vous informera de sa décision.</p>"
                    + "<p style='margin:0;color:#374151;font-size:15px;line-height:1.75'>"
                    + "Si vous ne recevez pas encore de réponse, vous pouvez consulter l'état de votre demande sur "
                    + lienRouge(urlPlateforme + "/candidat/suivi", "la page de suivi de candidature") + ".</p>"
                    + "</div>"
                    + piedEmail();

            helper.setText(contenu, true);
            ajouterLogo(helper);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi accusé de réception : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerConfirmationAdmission(String destinataire,
                                              String prenomNom,
                                              String emailCompte,
                                              String motDePasse) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = creerMessageDeMarque(
                    message,
                    destinataire,
                    "Votre demande de stage a été validée");

            String contenu = enteteEmail()

                + "<div style='background: white; padding: 35px; "
                    + "border: 1px solid #e2e8f0; border-top: none;'>"

                + "<div style='background: #f0fdf4; border: 2px solid #86efac; "
                    + "border-radius: 10px; padding: 20px; text-align: center; "
                    + "margin-bottom: 25px;'>"
                + "<h2 style='color: #16a34a; font-size: 22px; margin: 0 0 8px;'>"
                    + "VOTRE DEMANDE DE STAGE A ETE VALIDEE</h2>"
                + "<p style='color: #15803d; margin: 0; font-size: 15px;'>"
                    + "Felicitations !</p>"
                + "</div>"

                + "<p style='color: #374151; font-size: 15px;'>Bonjour "
                    + "<strong>" + echapperHtml(prenomNom) + "</strong>,</p>"
                + "<p style='color: #374151; font-size: 14px; line-height: 1.7; "
                    + "margin-top: 12px;'>"
                    + "Nous avons le plaisir de vous informer que votre demande "
                    + "de stage a ete acceptee. Votre compte a ete cree sur "
                    + "la plateforme.</p>"

                + "<div style='background: #f8fafc; border: 1px solid #e2e8f0; "
                    + "border-radius: 10px; padding: 20px; margin: 20px 0;'>"
                + "<h3 style='color: #1a2236; font-size: 15px; "
                    + "margin: 0 0 15px;'>Vos identifiants de connexion</h3>"
                + "<div style='margin-bottom: 10px;'>"
                + "<span style='color: #64748b; font-size: 13px;'>Adresse email</span>"
                + "<div style='color: #0f172a; font-weight: bold; "
                    + "font-size: 15px; margin-top: 4px;'>"
                    + echapperHtml(emailCompte) + "</div>"
                + "</div>"
                + "<div>"
                + "<span style='color: #64748b; font-size: 13px;'>Mot de passe</span>"
                + "<div style='color: #0f172a; font-weight: bold; "
                    + "font-size: 15px; margin-top: 4px; "
                    + "background: #e0f2fe; padding: 8px 12px; "
                    + "border-radius: 6px; display: inline-block;'>"
                    + echapperHtml(motDePasse) + "</div>"
                + "</div>"
                + "</div>"

                + "<p style='color: #374151; font-size: 14px; line-height: 1.7;'>"
                    + "Connectez-vous sur la plateforme avec ces identifiants. "
                    + "Nous vous recommandons de changer votre mot de passe "
                    + "lors de votre premiere connexion.</p>"

                + "<p style='margin:24px 0 0;color:#374151;font-size:14px;line-height:1.7'>"
                + "Pour vous connecter, utilisez "
                + lienRouge(urlPlateforme + "/login", "ce lien vers la plateforme DTA Alliance") + ".</p>"

                + "</div>"
                + piedEmail();

            helper.setText(contenu, true);
            ajouterLogo(helper);
            mailSender.send(message);
            System.out.println(">>> Email envoye a : " + destinataire);
            return true;

        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerRefusDemande(String destinataire,
                                    String prenomNom,
                                    String motifRefus) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = creerMessageDeMarque(
                    message,
                    destinataire,
                    "Réponse à votre demande de stage");

            String contenu = enteteEmail()
                    + "<div style='background:white;padding:35px;border:1px solid #e2e8f0;border-top:0'>"
                    + "<div style='background:#fef2f2;border:2px solid #fecaca;border-radius:10px;padding:20px;text-align:center;margin-bottom:25px'>"
                    + "<h2 style='color:#dc2626;font-size:21px;margin:0 0 8px'>RÉPONSE À VOTRE DEMANDE DE STAGE</h2>"
                    + "<p style='color:#b91c1c;margin:0'>Votre demande n'a pas été retenue</p></div>"
                    + "<p style='color:#374151;font-size:15px'>Bonjour <strong>" + echapperHtml(prenomNom) + "</strong>,</p>"
                    + "<p style='color:#374151;font-size:14px;line-height:1.7'>Après étude de votre dossier, nous sommes au regret de vous informer que votre demande de stage ne peut pas être acceptée.</p>"
                    + "<div style='background:#f8fafc;border-left:4px solid #ef4444;border-radius:8px;padding:18px;margin:22px 0'>"
                    + "<div style='color:#64748b;font-size:12px;font-weight:bold;text-transform:uppercase;margin-bottom:7px'>Motif</div>"
                    + "<div style='color:#1f2937;font-size:14px;line-height:1.6'>" + echapperHtml(motifRefus) + "</div></div>"
                    + "<p style='color:#374151;font-size:14px;line-height:1.7'>Nous vous remercions pour l'intérêt porté à notre structure et vous souhaitons une bonne continuation.</p>"
                    + "<p style='margin:24px 0 0;color:#374151;font-size:14px;line-height:1.7'>"
                    + "Vous pouvez revenir sur "
                    + lienRouge(urlPlateforme + "/login", "la plateforme DTA Alliance") + ".</p>"
                    + "</div>"
                    + piedEmail();

            helper.setText(contenu, true);
            ajouterLogo(helper);
            mailSender.send(message);
            System.out.println(">>> Email de refus envoyé à : " + destinataire);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de refus : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerChangementRole(String destinataire,
                                      String nomComplet,
                                      String telephone,
                                      String role,
                                      java.util.Collection<String> permissions) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur());
            helper.setTo(destinataire);
            helper.setSubject("Mise à jour de votre rôle sur Gestion des Stages");

            String listePermissions = permissions == null || permissions.isEmpty()
                    ? "<li style='color:#64748b'>Aucune permission spécifique</li>"
                    : permissions.stream()
                    .map(permission -> "<li style='margin:6px 0;color:#334155'>" + echapperHtml(permission) + "</li>")
                    .collect(java.util.stream.Collectors.joining());

            String contenu = "<div style='font-family:Arial,sans-serif;max-width:640px;margin:auto;padding:20px'>"
                    + "<div style='background:#1d4ed8;color:white;padding:24px;border-radius:10px 10px 0 0'>"
                    + "<h1 style='font-size:20px;margin:0'>Mise à jour de votre compte</h1></div>"
                    + "<div style='border:1px solid #dbe4f0;border-top:0;padding:28px;background:white'>"
                    + "<p>Bonjour <strong>" + echapperHtml(nomComplet) + "</strong>,</p>"
                    + "<p style='line-height:1.6;color:#334155'>Votre rôle et vos droits d'accès viennent d'être mis à jour par un administrateur.</p>"
                    + "<div style='background:#f8fafc;border-radius:8px;padding:18px;margin:20px 0'>"
                    + "<p style='margin:0 0 8px'><strong>Nom :</strong> " + echapperHtml(nomComplet) + "</p>"
                    + "<p style='margin:0 0 8px'><strong>E-mail :</strong> " + echapperHtml(destinataire) + "</p>"
                    + "<p style='margin:0 0 8px'><strong>Téléphone :</strong> " + echapperHtml(telephone == null || telephone.isBlank() ? "Non renseigné" : telephone) + "</p>"
                    + "<p style='margin:0'><strong>Nouveau rôle :</strong> " + echapperHtml(role) + "</p></div>"
                    + "<h2 style='font-size:16px;color:#0f172a'>Permissions associées</h2>"
                    + "<ul style='padding-left:20px'>" + listePermissions + "</ul>"
                    + "<p style='margin-top:22px;color:#475569;line-height:1.6'>Ces nouveaux droits seront appliqués à votre compte. Si vous êtes déjà connecté, reconnectez-vous afin de renouveler votre session.</p>"
                    + "</div></div>";

            helper.setText(contenu, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de changement de rôle : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerDecisionDemande(String destinataire,
                                           String nomComplet,
                                           boolean acceptee,
                                           String motif) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur());
            helper.setTo(destinataire);
            helper.setSubject(acceptee
                    ? "Votre demande de stage a été acceptée"
                    : "Décision concernant votre demande de stage");

            String couleur = acceptee ? "#16a34a" : "#dc2626";
            String titre = acceptee ? "Demande acceptée" : "Demande refusée";
            String detail = acceptee
                    ? "Votre demande a été acceptée. Le service chargé des stages vous communiquera les prochaines étapes."
                    : "Votre demande n'a pas été retenue."
                    + (motif == null || motif.isBlank()
                    ? ""
                    : "<br><br><strong>Motif :</strong> " + echapperHtml(motif));

            String contenu = "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;padding:20px'>"
                    + "<div style='background:#1d4ed8;color:white;padding:22px;border-radius:10px 10px 0 0'>"
                    + "<h1 style='font-size:20px;margin:0'>Gestion des Stages</h1></div>"
                    + "<div style='border:1px solid #dbe4f0;border-top:0;padding:28px;background:white'>"
                    + "<h2 style='color:" + couleur + ";margin-top:0'>" + titre + "</h2>"
                    + "<p>Bonjour <strong>" + echapperHtml(nomComplet) + "</strong>,</p>"
                    + "<p style='line-height:1.65;color:#334155'>" + detail + "</p>"
                    + "</div></div>";
            helper.setText(contenu, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de décision : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerLienReinitialisation(String destinataire,
                                                String nomComplet,
                                                String lien) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur());
            helper.setTo(destinataire);
            helper.setSubject("Réinitialisation de votre mot de passe DTA Alliance");
            String contenu = "<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;padding:20px'>"
                    + "<div style='background:#071b4d;color:white;padding:24px;border-radius:12px 12px 0 0'>"
                    + "<h1 style='font-size:20px;margin:0'>DTA Alliance</h1></div>"
                    + "<div style='padding:30px;border:1px solid #dbe4f0;border-top:0;background:white'>"
                    + "<p>Bonjour <strong>" + echapperHtml(nomComplet) + "</strong>,</p>"
                    + "<p style='color:#475569;line-height:1.65'>Une demande de réinitialisation a été effectuée pour votre compte. Ce lien est personnel, utilisable une seule fois et expire dans 30 minutes.</p>"
                    + "<p style='margin:28px 0;text-align:center'><a href='" + echapperHtml(lien) + "' "
                    + "style='display:inline-block;padding:13px 24px;color:white;background:#2563eb;border-radius:9px;text-decoration:none;font-weight:bold'>"
                    + "Réinitialiser mon mot de passe</a></p>"
                    + "<p style='color:#64748b;font-size:13px'>Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email.</p>"
                    + "</div></div>";
            helper.setText(contenu, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de réinitialisation : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerStatutCompte(String destinataire,
                                       String nomComplet,
                                       boolean bloque) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur());
            helper.setTo(destinataire);
            helper.setSubject(bloque
                    ? "Blocage de votre compte DTA Alliance"
                    : "Réactivation de votre compte DTA Alliance");
            java.time.LocalDateTime maintenant = java.time.LocalDateTime.now();
            String date = maintenant.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String heure = maintenant.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String action = bloque
                    ? "Votre compte a été bloqué le " + date + " à " + heure
                            + ".<br>Vous n'avez plus accès à votre espace."
                    : "Votre compte a été débloqué le " + date + " à " + heure
                            + ".<br>Vous pouvez de nouveau accéder à votre espace.";
            helper.setText("<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;padding:20px'>"
                    + "<div style='background:#071b4d;color:white;padding:24px;border-radius:12px 12px 0 0'>"
                    + "<h1 style='font-size:20px;margin:0'>DTA Alliance</h1></div>"
                    + "<div style='padding:30px;border:1px solid #dbe4f0;border-top:0;background:white'>"
                    + "<p>Bonjour <strong>" + echapperHtml(nomComplet) + "</strong>,</p>"
                    + "<p style='color:#475569;line-height:1.65'>" + action + "</p>"
                    + "<p style='margin-top:28px;color:#64748b'>Administration DTA Alliance</p>"
                    + "</div></div>", true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de statut du compte : " + e.getMessage());
            return false;
        }
    }

    public boolean envoyerNotificationSimple(String destinataire, String sujet, String messageTexte) {
        if (!destinataireValide(destinataire)) return false;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(expediteur());
            helper.setTo(destinataire);
            helper.setSubject(sujet);
            helper.setText("<div style='font-family:Arial,sans-serif;max-width:620px;margin:auto;padding:24px'>"
                    + "<h2 style='color:#1d4ed8'>Gestion des Stages</h2>"
                    + "<p style='line-height:1.7;color:#334155'>"
                    + echapperHtml(messageTexte).replace("\n", "<br>") + "</p></div>", true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println(">>> Erreur email notification : " + e.getMessage());
            return false;
        }
    }

    private MimeMessageHelper creerMessageDeMarque(MimeMessage message,
                                                    String destinataire,
                                                    String sujet) throws Exception {
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(expediteur());
        helper.setTo(destinataire);
        helper.setSubject(sujet);
        return helper;
    }

    private String enteteEmail() {
        return "<div style=\"font-family:'Segoe UI',Roboto,Arial,sans-serif;max-width:620px;margin:0 auto;padding:20px\">"
                + "<div style='background:#071b4d;padding:22px 30px;border-radius:12px 12px 0 0;text-align:center'>"
                + "<img src='cid:logoDta' alt='DTA Alliance' "
                + "style='display:block;max-width:150px;max-height:70px;margin:0 auto 12px'>"
                + "<div style='color:#ffffff;font-size:20px;font-weight:700'>DTA Alliance</div>"
                + "<div style='color:#cbd5e1;font-size:13px;margin-top:4px'>"
                + "Plateforme de Gestion des Stages</div></div>";
    }

    private String piedEmail() {
        return "<div style='background:#f8fafc;padding:16px;border:1px solid #e2e8f0;border-top:0;"
                + "border-radius:0 0 12px 12px;text-align:center'>"
                + "<p style='color:#64748b;font-size:12px;margin:0'>"
                + "© 2026 DTA Alliance — Plateforme de Gestion des Stages</p></div></div>";
    }

    private String lienRouge(String url, String libelle) {
        return "<a href='" + echapperHtml(url)
                + "' style='color:#dc2626;font-weight:700;text-decoration:underline'>"
                + echapperHtml(libelle) + "</a>";
    }

    private void ajouterLogo(MimeMessageHelper helper) throws Exception {
        helper.addInline("logoDta", new ClassPathResource("static/images/logo.png"), "image/png");
    }

    private String echapperHtml(String valeur) {
        if (valeur == null) return "";
        return valeur.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean destinataireValide(String destinataire) {
        return destinataire != null
                && destinataire.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
