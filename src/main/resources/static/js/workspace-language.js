(() => {
    const storageKey = 'dta-workspace-language';
    const excludedSelector = [
        'script', 'style', 'code', 'pre',
        '.um-messages', '.um-message',
        '[data-no-translate]', '[contenteditable="true"]'
    ].join(',');
    const originalText = new WeakMap();
    const originalAttributes = new WeakMap();
    let currentLanguage = localStorage.getItem(storageKey) === 'en' ? 'en' : 'fr';
    let applying = false;

    const phrases = {
        'Gestion des stages': 'Internship management',
        'Tableau de bord': 'Dashboard',
        'Espace de travail': 'Workspace',
        'Demandes de stage': 'Internship applications',
        'Liste des demandes': 'Application list',
        'Nouvelle conversation': 'New conversation',
        'Nouvelle évaluation': 'New evaluation',
        'Nouvel objectif': 'New objective',
        'Nouvelle tâche': 'New task',
        'Choisir un contact': 'Choose a contact',
        'Premier message': 'First message',
        'Rechercher par nom ou email': 'Search by name or email',
        'Rechercher dans la page': 'Search this page',
        'Rechercher dans cette page...': 'Search this page...',
        'Rechercher dans l’espace responsable': 'Search the manager workspace',
        'Rechercher dans l\'espace responsable': 'Search the manager workspace',
        'Échangez des messages et des documents avec vos contacts.': 'Exchange messages and documents with your contacts.',
        'Écrivez votre message...': 'Write your message...',
        'Afficher les contacts': 'Show contacts',
        'Tout marquer comme lu': 'Mark all as read',
        'Voir toutes les notifications': 'View all notifications',
        'Aucune notification pour le moment.': 'No notifications for now.',
        'Aucune nouvelle notification.': 'No new notifications.',
        'Les notifications sont momentanément indisponibles.': 'Notifications are temporarily unavailable.',
        'Créer un compte': 'Create an account',
        'Mot de passe temporaire': 'Temporary password',
        'Terminer et envoyer': 'Finish and send',
        'Refuser et envoyer': 'Reject and send',
        'Motif du refus': 'Reason for rejection',
        'Voir plus grand': 'View larger',
        'Voir les détails': 'View details',
        'Voir détails': 'View details',
        'Détails du document': 'Document details',
        'Détails du stagiaire': 'Intern details',
        'Détails de la demande': 'Application details',
        'Informations du candidat': 'Candidate information',
        'Documents joints': 'Attached documents',
        'Période souhaitée': 'Requested period',
        'Service demandé': 'Requested department',
        'Date de début': 'Start date',
        'Date de fin': 'End date',
        'Début & Fin': 'Start & End',
        'Actions rapides': 'Quick actions',
        'Prochaines échéances': 'Upcoming deadlines',
        'Analyse des objectifs': 'Objective analysis',
        'Historique des versions': 'Version history',
        'Historique des consultations': 'Viewing history',
        'Taux de réalisation': 'Completion rate',
        'Objectifs totaux': 'Total objectives',
        'Objectifs atteints': 'Objectives achieved',
        'Objectifs en cours': 'Objectives in progress',
        'Objectifs en retard': 'Overdue objectives',
        'Stagiaires concernés': 'Interns concerned',
        'Gestion des tâches': 'Task management',
        'Mes stagiaires': 'My interns',
        'Mes tâches': 'My tasks',
        'Mes messages': 'My messages',
        'Mon profil': 'My profile',
        'Journal de bord': 'Activity journal',
        'Journal d’activité': 'Activity log',
        'Journal d\'activité': 'Activity log',
        'Clôture des stages': 'Internship closure',
        'Rapport final': 'Final report',
        'Rapports hebdomadaires': 'Weekly reports',
        'Fiche de notes': 'Grade sheet',
        'Attestation de stage': 'Internship certificate',
        'Rôles & permissions': 'Roles & permissions',
        'Sauvegarde et restauration': 'Backup and restore',
        'Paramètres de sécurité': 'Security settings',
        'Mode d’affichage': 'Display mode',
        'Mode d\'affichage': 'Display mode',
        'Importer un document': 'Import a document',
        'Télécharger le document': 'Download document',
        'Déposer un livrable': 'Submit a deliverable',
        'Ajouter un document': 'Add a document',
        'Ajouter une pièce jointe': 'Add an attachment',
        'Ajouter des images': 'Add images',
        'Ajouter un emoji': 'Add an emoji',
        'Filtrer les conversations': 'Filter conversations',
        'Changer la langue': 'Change language',
        'Déconnexion': 'Sign out',
        'Administrateurs': 'Administrators',
        'Responsables': 'Managers',
        'Encadreurs': 'Supervisors',
        'Stagiaires': 'Interns',
        'Administrateur': 'Administrator',
        'Responsable des stages': 'Internship manager',
        'Responsable': 'Manager',
        'Encadreur': 'Supervisor',
        'Stagiaire': 'Intern',
        'Candidatures': 'Applications',
        'Demandes': 'Applications',
        'Utilisateurs': 'Users',
        'Services': 'Departments',
        'Documents': 'Documents',
        'Rapports': 'Reports',
        'Messagerie': 'Messages',
        'Messages': 'Messages',
        'Notifications': 'Notifications',
        'Paramètres': 'Settings',
        'Objectifs': 'Objectives',
        'Tâches': 'Tasks',
        'Livrables': 'Deliverables',
        'Évaluations': 'Evaluations',
        'Planning': 'Schedule',
        'Dossiers': 'Files',
        'Archives': 'Archives',
        'Admissions': 'Admissions',
        'Affectations': 'Assignments',
        'Suivi': 'Monitoring',
        'Accueil': 'Home',
        'Recherche': 'Search',
        'Rechercher...': 'Search...',
        'Rechercher': 'Search',
        'Filtres': 'Filters',
        'Filtrer': 'Filter',
        'Réinitialiser': 'Reset',
        'Appliquer': 'Apply',
        'Tous': 'All',
        'Toutes': 'All',
        'Aujourd’hui': 'Today',
        'Aujourd\'hui': 'Today',
        'Cette semaine': 'This week',
        'Ce mois': 'This month',
        'Mois': 'Month',
        'Semaine': 'Week',
        'Jour': 'Day',
        'Nom': 'Name',
        'Prénom': 'First name',
        'Email': 'Email',
        'Téléphone': 'Phone',
        'Adresse': 'Address',
        'Établissement': 'Institution',
        'Filière': 'Program',
        'Niveau': 'Level',
        'Service': 'Department',
        'Encadreur assigné': 'Assigned supervisor',
        'Période': 'Period',
        'Durée': 'Duration',
        'Date': 'Date',
        'Heure': 'Time',
        'Titre': 'Title',
        'Description': 'Description',
        'Commentaire': 'Comment',
        'Commentaires': 'Comments',
        'Auteur': 'Author',
        'Version': 'Version',
        'Statut': 'Status',
        'Priorité': 'Priority',
        'Progression': 'Progress',
        'Moyenne': 'Average',
        'Action': 'Action',
        'Actions': 'Actions',
        'Actif': 'Active',
        'Inactive': 'Inactive',
        'Inactif': 'Inactive',
        'En ligne': 'Online',
        'Hors ligne': 'Offline',
        'En attente': 'Pending',
        'En cours': 'In progress',
        'En retard': 'Overdue',
        'Terminé': 'Completed',
        'Terminée': 'Completed',
        'Acceptée': 'Accepted',
        'Refusée': 'Rejected',
        'Validé': 'Approved',
        'Rejeté': 'Rejected',
        'Déposé': 'Submitted',
        'Basse': 'Low',
        'Moyenne': 'Medium',
        'Haute': 'High',
        'Plus récent': 'Newest',
        'Plus ancien': 'Oldest',
        'Créer': 'Create',
        'Ajouter': 'Add',
        'Modifier': 'Edit',
        'Supprimer': 'Delete',
        'Bloquer': 'Block',
        'Débloquer': 'Unblock',
        'Accepter': 'Accept',
        'Refuser': 'Reject',
        'Valider': 'Approve',
        'Envoyer': 'Send',
        'Télécharger': 'Download',
        'Importer': 'Import',
        'Exporter': 'Export',
        'Enregistrer': 'Save',
        'Générer': 'Generate',
        'Annuler': 'Cancel',
        'Fermer': 'Close',
        'Retour': 'Back',
        'Continuer': 'Continue',
        'Précédent': 'Previous',
        'Suivant': 'Next',
        'Ouvrir': 'Open',
        'Consulter': 'View',
        'Aperçu': 'Preview',
        'Sélectionner': 'Select',
        'Aucun contact disponible.': 'No contact available.',
        'Chargement...': 'Loading...',
        'Compte actif': 'Active account',
        'Compte inactif': 'Inactive account',
        'Succès': 'Success',
        'Erreur': 'Error'
    };
    const escapeRegExp = value => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const replacements = Object.entries(phrases)
        .sort((a, b) => b[0].length - a[0].length)
        .map(([fr, en]) => [
            en,
            new RegExp(
                `(?<![\\p{L}\\p{N}])${escapeRegExp(fr)}(?![\\p{L}\\p{N}])`,
                'gu'
            )
        ]);

    function isExcluded(node) {
        const element = node.nodeType === Node.ELEMENT_NODE ? node : node.parentElement;
        return !element || Boolean(element.closest(excludedSelector));
    }

    function translateValue(value) {
        if (!value || currentLanguage === 'fr') return value;
        if (phrases[value]) return phrases[value];
        let translated = value;
        replacements.forEach(([en, expression]) => {
            expression.lastIndex = 0;
            translated = translated.replace(expression, en);
        });
        return translated;
    }

    function translateTextNode(node) {
        if (isExcluded(node)) return;
        if (!originalText.has(node)) originalText.set(node, node.nodeValue);
        const source = originalText.get(node);
        const leading = source.match(/^\s*/)?.[0] || '';
        const trailing = source.match(/\s*$/)?.[0] || '';
        const content = source.trim();
        node.nodeValue = content ? leading + translateValue(content) + trailing : source;
    }

    function translateElementAttributes(element) {
        if (isExcluded(element)) return;
        const names = ['placeholder', 'title', 'aria-label', 'data-label'];
        let originals = originalAttributes.get(element);
        if (!originals) {
            originals = {};
            names.forEach(name => {
                if (element.hasAttribute(name)) originals[name] = element.getAttribute(name);
            });
            originalAttributes.set(element, originals);
        }
        Object.entries(originals).forEach(([name, value]) => {
            element.setAttribute(name, translateValue(value));
        });
    }

    function refreshLanguageControls() {
        document.documentElement.lang = currentLanguage;
        document.querySelectorAll('.workspace-language-button span')
            .forEach(label => { label.textContent = currentLanguage.toUpperCase(); });
    }

    function applyLanguage(root = document.body, forceRestore = false) {
        if (!root || applying) return;
        if (currentLanguage === 'fr' && !forceRestore) {
            refreshLanguageControls();
            return;
        }
        applying = true;
        try {
            if (root.nodeType === Node.TEXT_NODE) {
                translateTextNode(root);
            } else {
                translateElementAttributes(root);
                const textWalker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
                while (textWalker.nextNode()) translateTextNode(textWalker.currentNode);
                root.querySelectorAll?.('*').forEach(translateElementAttributes);
            }
            refreshLanguageControls();
        } finally {
            applying = false;
        }
    }

    function attachButtons() {
        document.querySelectorAll('.workspace-language-button').forEach(button => {
            if (button.dataset.languageReady === 'true') return;
            button.dataset.languageReady = 'true';
            button.addEventListener('click', () => {
                currentLanguage = currentLanguage === 'fr' ? 'en' : 'fr';
                localStorage.setItem(storageKey, currentLanguage);
                applyLanguage(document.body, true);
                document.dispatchEvent(new CustomEvent('workspace:languagechange', {
                    detail: {language: currentLanguage}
                }));
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        attachButtons();
        applyLanguage(document.body);

        const pendingNodes = new Set();
        let translationFrame = 0;
        const flushPendingNodes = () => {
            translationFrame = 0;
            if (currentLanguage !== 'en') {
                pendingNodes.clear();
                attachButtons();
                return;
            }
            const nodes = [...pendingNodes];
            pendingNodes.clear();
            nodes.forEach(node => {
                const parentAlreadyQueued = nodes.some(candidate =>
                    candidate !== node
                    && candidate.nodeType === Node.ELEMENT_NODE
                    && candidate.contains(node)
                );
                if (!parentAlreadyQueued) applyLanguage(node);
            });
            attachButtons();
        };

        const observer = new MutationObserver(mutations => {
            if (applying) return;
            mutations.forEach(mutation => {
                mutation.addedNodes.forEach(node => {
                    if (node.nodeType === Node.TEXT_NODE || node.nodeType === Node.ELEMENT_NODE) {
                        pendingNodes.add(node);
                    }
                });
            });
            if (!translationFrame) {
                translationFrame = window.requestAnimationFrame(flushPendingNodes);
            }
        });
        observer.observe(document.body, {childList: true, subtree: true});
    });
})();
