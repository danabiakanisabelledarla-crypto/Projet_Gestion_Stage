(() => {
    const storageKey = 'dta-workspace-language';
    const translations = new Map(Object.entries({
        'Tableau de bord': 'Dashboard',
        'Demandes': 'Applications',
        'Stagiaires': 'Interns',
        'Utilisateurs': 'Users',
        'Services': 'Services',
        'Rôles & permissions': 'Roles & permissions',
        'Documents': 'Documents',
        'Rapports': 'Reports',
        'Messagerie': 'Messages',
        'Messages': 'Messages',
        'Notifications': 'Notifications',
        'Journal d’activité': 'Activity log',
        'Paramètres': 'Settings',
        'Mon profil': 'My profile',
        'Mes stagiaires': 'My interns',
        'Objectifs': 'Objectives',
        'Tâches': 'Tasks',
        'Mes tâches': 'My tasks',
        'Livrables': 'Deliverables',
        'Évaluations': 'Evaluations',
        'Planning': 'Schedule',
        'Journal de bord': 'Activity journal',
        'Rapport final': 'Final report',
        'Dossiers': 'Files',
        'Clôture des stages': 'Internship closure',
        'Accueil': 'Home',
        'Responsable': 'Manager',
        'Encadreur': 'Supervisor',
        'Administrateur': 'Administrator',
        'Stagiaire': 'Intern',
        'Espace de travail': 'Workspace',
        'Gestion des stages': 'Internship management',
        'Compte actif': 'Active account',
        'Rechercher': 'Search',
        'Rechercher...': 'Search...',
        'Filtres': 'Filters',
        'Réinitialiser': 'Reset',
        'Voir détails': 'View details',
        'Modifier le profil': 'Edit profile',
        'Gérer les permissions': 'Manage permissions',
        'Voir les stagiaires': 'View interns',
        'Désactiver le compte': 'Disable account',
        'Télécharger': 'Download',
        'Importer un document': 'Import a document',
        'Annuler': 'Cancel',
        'Enregistrer': 'Save',
        'Fermer': 'Close',
        'Accepter': 'Accept',
        'Refuser': 'Reject',
        'En attente': 'Pending',
        'Acceptée': 'Accepted',
        'Refusée': 'Rejected',
        'Plus récent': 'Newest',
        'Plus ancien': 'Oldest',
        'Nom': 'Name',
        'Statut': 'Status',
        'Déconnexion': 'Sign out'
    }));
    const reverseTranslations = new Map([...translations].map(([fr, en]) => [en, fr]));

    function translateText(root, language) {
        const dictionary = language === 'en' ? translations : reverseTranslations;
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        const nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);
        nodes.forEach(node => {
            if (node.parentElement?.closest('script, style, textarea')) return;
            const value = node.nodeValue.trim();
            if (!dictionary.has(value)) return;
            node.nodeValue = node.nodeValue.replace(value, dictionary.get(value));
        });
        document.querySelectorAll('input[placeholder], textarea[placeholder]').forEach(field => {
            const placeholder = field.getAttribute('placeholder');
            if (dictionary.has(placeholder)) field.setAttribute('placeholder', dictionary.get(placeholder));
        });
        document.documentElement.lang = language;
    }

    document.addEventListener('DOMContentLoaded', () => {
        const header = document.querySelector('.admin-header, .coach-topbar, .responsable-topbar, .intern-header');
        if (!header || header.querySelector('.workspace-language-button')) return;
        let current = localStorage.getItem(storageKey) || 'fr';
        translateText(document.body, current);

        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'workspace-language-button';
        button.innerHTML = `<i class="fa-solid fa-language"></i><span>${current.toUpperCase()}</span>`;
        button.title = 'Français / English';
        button.setAttribute('aria-label', button.title);

        const actions = header.querySelector('.header-droite, .responsable-tools, .intern-header-actions');
        if (actions) actions.prepend(button);
        else header.appendChild(button);

        button.addEventListener('click', () => {
            const next = current === 'fr' ? 'en' : 'fr';
            translateText(document.body, next);
            current = next;
            localStorage.setItem(storageKey, current);
            button.querySelector('span').textContent = current.toUpperCase();
        });
    });
})();
