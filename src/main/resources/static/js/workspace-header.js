document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-workspace-header]').forEach(header => {
        if (header.dataset.ready === 'true') return;
        header.dataset.ready = 'true';

        const profileTrigger = header.querySelector('[data-profile-trigger]');
        const profilePanel = header.querySelector('[data-profile-panel]');
        const search = header.querySelector('.intern-search input');
        const notificationTrigger = header.querySelector('.workspace-notification-trigger');
        const notificationPanel = header.querySelector('[data-notification-panel]');
        const notificationList = header.querySelector('[data-notification-list]');
        const notificationCount = header.querySelector('[data-notification-count]');

        profileTrigger?.addEventListener('click', event => {
            event.stopPropagation();
            const open = !profilePanel.classList.contains('intern-popover-open');
            profilePanel.classList.toggle('intern-popover-open', open);
            profileTrigger.setAttribute('aria-expanded', String(open));
        });

        document.addEventListener('click', event => {
            if (!event.target.closest('[data-profile-trigger], [data-profile-panel]')) {
                profilePanel?.classList.remove('intern-popover-open');
                profileTrigger?.setAttribute('aria-expanded', 'false');
            }
        });

        const normalize = value => (value || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLowerCase()
            .trim();

        search?.addEventListener('input', () => {
            const query = normalize(search.value);
            document.querySelectorAll(
                'main tbody tr, main [data-search-item], main .backup-card, main .stat-card, '
                + 'main .rp-card, main .coach-card, main .intern-card, main .kanban-card, '
                + 'main .journal-entry, main .um-conversation'
            ).forEach(item => {
                item.classList.toggle(
                    'intern-search-hidden',
                    Boolean(query) && !normalize(item.textContent).includes(query)
                );
            });
        });

        const escapeHtml = value => {
            const node = document.createElement('div');
            node.textContent = value || '';
            return node.innerHTML;
        };
        const formatNotificationDate = value => {
            if (!value) return '';
            const date = new Date(value);
            return Number.isNaN(date.getTime()) ? '' :
                new Intl.DateTimeFormat('fr-FR', {dateStyle: 'short', timeStyle: 'short'}).format(date);
        };
        const renderNotifications = notifications => {
            if (!notificationList) return;
            notificationList.innerHTML = notifications.length
                ? notifications.map(item => `<article class="workspace-notification-item">
                    <strong>${escapeHtml(item.objet)}</strong>
                    <span>${escapeHtml(item.message)}</span>
                    <small>${formatNotificationDate(item.date)}</small>
                </article>`).join('')
                : '<p class="workspace-notification-empty">Aucune notification.</p>';
        };
        const markNotificationsRead = async () => {
            const url = notificationTrigger?.dataset.notificationRead;
            if (!url) return;
            try {
                const headers = {'X-Requested-With': 'XMLHttpRequest'};
                const csrfHeader = notificationTrigger.dataset.csrfHeader;
                const csrfToken = notificationTrigger.dataset.csrfToken;
                if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
                const response = await fetch(url, {
                    method: 'POST',
                    headers
                });
                if (response.ok && notificationCount) {
                    notificationCount.textContent = '0';
                    notificationCount.classList.add('is-empty');
                }
            } catch (error) {
                // Le menu reste utilisable même si le marquage est indisponible.
            }
        };
        notificationTrigger?.addEventListener('click', async event => {
            event.preventDefault();
            event.stopPropagation();
            const open = notificationPanel?.hidden !== false;
            if (!notificationPanel) return;
            notificationPanel.hidden = !open;
            notificationTrigger.setAttribute('aria-expanded', String(open));
            if (!open) return;
            const apiUrl = notificationTrigger.dataset.notificationApi;
            try {
                const response = await fetch(apiUrl, {headers: {'X-Requested-With': 'XMLHttpRequest'}});
                if (!response.ok) throw new Error('notifications');
                renderNotifications(await response.json());
                await markNotificationsRead();
            } catch (error) {
                if (notificationList) notificationList.innerHTML = '<p class="workspace-notification-empty">Notifications indisponibles.</p>';
            }
        });
        document.addEventListener('click', event => {
            if (!event.target.closest('.workspace-notification-panel, .workspace-notification-trigger')) {
                if (notificationPanel) notificationPanel.hidden = true;
                notificationTrigger?.setAttribute('aria-expanded', 'false');
            }
        });

        document.addEventListener('keydown', event => {
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && search) {
                event.preventDefault();
                if (window.matchMedia('(max-width: 700px)').matches) {
                    header.classList.add('search-open');
                }
                search.focus();
            }
            if (event.key === 'Escape') {
                header.classList.remove('search-open');
                profilePanel?.classList.remove('intern-popover-open');
            }
        });

        header.querySelector('.intern-brand')?.addEventListener('contextmenu', event => {
            if (!window.matchMedia('(max-width: 700px)').matches) return;
            event.preventDefault();
            header.classList.toggle('search-open');
            if (header.classList.contains('search-open')) search?.focus();
        });
    });
});
