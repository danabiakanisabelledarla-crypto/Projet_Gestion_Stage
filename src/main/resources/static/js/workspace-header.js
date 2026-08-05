document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-workspace-header]').forEach(header => {
        if (header.dataset.ready === 'true') return;
        header.dataset.ready = 'true';

        const profileTrigger = header.querySelector('[data-profile-trigger]');
        const profilePanel = header.querySelector('[data-profile-panel]');
        const search = header.querySelector('.intern-search input');

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
