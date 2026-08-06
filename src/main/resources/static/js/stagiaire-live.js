(() => {
    if (window.__stagiaireLiveLoaded) return;
    window.__stagiaireLiveLoaded = true;

    const channel = 'BroadcastChannel' in window
        ? new BroadcastChannel('dta-stagiaire-sync')
        : null;
    let refreshing = false;

    const applyPreferences = preferences => {
        if (!preferences) return;
        const desiredLanguage = preferences.language === 'en' ? 'en' : 'fr';
        localStorage.setItem('dta-stagiaire-profile-preferences', JSON.stringify(preferences));
        document.body.classList.toggle('workspace-dark', Boolean(preferences.dark));
        document.documentElement.dataset.taskReminders = preferences.tasks ? 'enabled' : 'disabled';
        document.documentElement.dataset.systemNotifications =
            preferences.system ? 'enabled' : 'disabled';
        if (document.documentElement.lang !== desiredLanguage) {
            document.querySelector('.workspace-language-button')?.click();
        } else {
            localStorage.setItem('dta-workspace-language', desiredLanguage);
        }
    };

    async function loadPreferences() {
        try {
            const response = await fetch('/stagiaire/preferences', {
                credentials: 'same-origin',
                headers: {'Accept': 'application/json'}
            });
            if (response.ok) applyPreferences(await response.json());
        } catch (error) {
            try {
                applyPreferences(JSON.parse(
                    localStorage.getItem('dta-stagiaire-profile-preferences') || '{}'
                ));
            } catch (ignored) {
                // The defaults rendered by the server remain active.
            }
        }
    }

    async function refreshPage(url = window.location.href, announce = false) {
        if (refreshing) return;
        refreshing = true;
        document.body.classList.add('stagiaire-syncing');
        if (announce) {
            const timestamp = Date.now();
            localStorage.setItem('dta-stagiaire-last-sync', String(timestamp));
            channel?.postMessage({timestamp, path: location.pathname});
        }
        window.location.assign(url);
    }
    window.stagiaireLiveRefresh = refreshPage;

    /*
     * Native form submission is intentional here. Intercepting every POST with
     * fetch hid server redirects and validation errors, notably on file uploads.
     */
    /* document.addEventListener('submit', async event => {
        const form = event.target.closest('form');
        if (!form || event.defaultPrevented) return;
        const method = (form.method || 'get').toLowerCase();
        const action = new URL(form.action || location.href, location.href);
        if (method !== 'post'
                || action.origin !== location.origin
                || !action.pathname.startsWith('/stagiaire/')
                || action.pathname.includes('/messages')
                || action.pathname.endsWith('/logout')) {
            return;
        }
        event.preventDefault();
        const submitter = event.submitter;
        submitter?.setAttribute('disabled', 'disabled');
        form.setAttribute('aria-busy', 'true');
        try {
            const response = await fetch(action, {
                method: 'POST',
                body: new FormData(form),
                credentials: 'same-origin',
                headers: {'X-Requested-With': 'XMLHttpRequest'}
            });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            await refreshPage(response.url || window.location.href, true);
        } catch (error) {
            form.removeAttribute('aria-busy');
            submitter?.removeAttribute('disabled');
            window.alert('La mise à jour a échoué. Vérifiez les informations puis réessayez.');
        }
    }); */

    channel?.addEventListener('message', event => {
        if (document.visibilityState === 'visible'
                && location.pathname === '/stagiaire/dashboard') {
            refreshPage(location.href, false);
        }
    });

    window.addEventListener('pageshow', event => {
        if (event.persisted) refreshPage(location.href, false);
    });

    window.addEventListener('focus', () => {
        if (location.pathname !== '/stagiaire/dashboard') return;
        const lastSync = Number(localStorage.getItem('dta-stagiaire-last-sync') || 0);
        const lastLoad = Number(sessionStorage.getItem('dta-stagiaire-dashboard-load') || 0);
        if (lastSync > lastLoad) {
            sessionStorage.setItem('dta-stagiaire-dashboard-load', String(Date.now()));
            refreshPage(location.href, false);
        }
    });

    loadPreferences();
})();
