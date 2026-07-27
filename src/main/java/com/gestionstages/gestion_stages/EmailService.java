package com.gestionstages.gestion_stages;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void envoyerConfirmationAdmission(String destinataire,
                                              String prenomNom,
                                              String emailCompte,
                                              String motDePasse) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("innotechlab26@gmail.com");
            helper.setTo(destinataire);
            helper.setSubject("Votre demande de stage a ete validee");

            String contenu = "<div style='font-family: Arial, sans-serif; "
                    + "max-width: 600px; margin: 0 auto; padding: 20px;'>"

                + "<div style='background: #1a2236; padding: 25px; "
                    + "border-radius: 10px 10px 0 0; text-align: center;'>"
                + "<h1 style='color: white; font-size: 20px; margin: 0;'>"
                    + "Plateforme de Gestion des Stages</h1>"
                + "</div>"

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
                    + "<strong>" + prenomNom + "</strong>,</p>"
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
                    + emailCompte + "</div>"
                + "</div>"
                + "<div>"
                + "<span style='color: #64748b; font-size: 13px;'>Mot de passe</span>"
                + "<div style='color: #0f172a; font-weight: bold; "
                    + "font-size: 15px; margin-top: 4px; "
                    + "background: #e0f2fe; padding: 8px 12px; "
                    + "border-radius: 6px; display: inline-block;'>"
                    + motDePasse + "</div>"
                + "</div>"
                + "</div>"

                + "<p style='color: #374151; font-size: 14px; line-height: 1.7;'>"
                    + "Connectez-vous sur la plateforme avec ces identifiants. "
                    + "Nous vous recommandons de changer votre mot de passe "
                    + "lors de votre premiere connexion.</p>"

                + "<div style='margin-top: 25px; text-align: center;'>"
                + "<a href='http://localhost:8080/login' "
                    + "style='display: inline-block; padding: 14px 30px; "
                    + "background: #2563eb; color: white; text-decoration: none; "
                    + "border-radius: 8px; font-weight: bold; font-size: 15px;'>"
                    + "Acceder a mon espace</a>"
                + "</div>"

                + "</div>"

                + "<div style='background: #f8fafc; padding: 15px; "
                    + "border-radius: 0 0 10px 10px; text-align: center; "
                    + "border: 1px solid #e2e8f0; border-top: none;'>"
                + "<p style='color: #94a3b8; font-size: 12px; margin: 0;'>"
                    + "Plateforme de Gestion des Stages &copy; 2026</p>"
                + "</div>"

                + "</div>";

            helper.setText(contenu, true);
            mailSender.send(message);
            System.out.println(">>> Email envoye a : " + destinataire);

        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email : " + e.getMessage());
        }
    }

    public void envoyerRefusDemande(String destinataire,
                                    String prenomNom,
                                    String motifRefus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("innotechlab26@gmail.com");
            helper.setTo(destinataire);
            helper.setSubject("Réponse à votre demande de stage");

            String contenu = "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                    + "<div style='background:#1a2236;padding:25px;border-radius:10px 10px 0 0;text-align:center'>"
                    + "<h1 style='color:white;font-size:20px;margin:0'>Plateforme de Gestion des Stages</h1></div>"
                    + "<div style='background:white;padding:35px;border:1px solid #e2e8f0;border-top:0'>"
                    + "<div style='background:#fef2f2;border:2px solid #fecaca;border-radius:10px;padding:20px;text-align:center;margin-bottom:25px'>"
                    + "<h2 style='color:#dc2626;font-size:21px;margin:0 0 8px'>RÉPONSE À VOTRE DEMANDE DE STAGE</h2>"
                    + "<p style='color:#b91c1c;margin:0'>Votre demande n'a pas été retenue</p></div>"
                    + "<p style='color:#374151;font-size:15px'>Bonjour <strong>" + prenomNom + "</strong>,</p>"
                    + "<p style='color:#374151;font-size:14px;line-height:1.7'>Après étude de votre dossier, nous sommes au regret de vous informer que votre demande de stage ne peut pas être acceptée.</p>"
                    + "<div style='background:#f8fafc;border-left:4px solid #ef4444;border-radius:8px;padding:18px;margin:22px 0'>"
                    + "<div style='color:#64748b;font-size:12px;font-weight:bold;text-transform:uppercase;margin-bottom:7px'>Motif</div>"
                    + "<div style='color:#1f2937;font-size:14px;line-height:1.6'>" + motifRefus + "</div></div>"
                    + "<p style='color:#374151;font-size:14px;line-height:1.7'>Nous vous remercions pour l'intérêt porté à notre structure et vous souhaitons une bonne continuation.</p>"
                    + "</div><div style='background:#f8fafc;padding:15px;border:1px solid #e2e8f0;border-top:0;border-radius:0 0 10px 10px;text-align:center'>"
                    + "<p style='color:#94a3b8;font-size:12px;margin:0'>Plateforme de Gestion des Stages &copy; 2026</p></div></div>";

            helper.setText(contenu, true);
            mailSender.send(message);
            System.out.println(">>> Email de refus envoyé à : " + destinataire);
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de refus : " + e.getMessage());
        }
    }

    public void envoyerChangementRole(String destinataire,
                                      String nomComplet,
                                      String telephone,
                                      String role,
                                      java.util.Collection<String> permissions) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("innotechlab26@gmail.com");
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
        } catch (Exception e) {
            System.err.println(">>> Erreur envoi email de changement de rôle : " + e.getMessage());
        }
    }

    private String echapperHtml(String valeur) {
        if (valeur == null) return "";
        return valeur.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
