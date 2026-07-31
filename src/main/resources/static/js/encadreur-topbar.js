document.addEventListener('DOMContentLoaded', () => {
    const topbar = document.querySelector('.coach-topbar');
    if (!topbar) return;

    const search = topbar.querySelector('input[type="search"]');
    if (search && !search.dataset.coachSearchReady) {
        search.dataset.coachSearchReady = 'true';
        search.addEventListener('input', () => {
            const query = search.value.trim().toLowerCase();
            document.querySelectorAll('.coach-table tbody tr, .coach-card[data-searchable], .coach-timeline-item')
                .forEach(item => {
                    item.style.display = !query || item.textContent.toLowerCase().includes(query) ? '' : 'none';
                });
        });
    }

    const bell = [...topbar.querySelectorAll('.coach-icon-btn')]
        .find(button => button.querySelector('.fa-bell'));
    if (!bell) return;
    bell.type = 'button';
    bell.title = 'Notifications';

    const panel = document.createElement('div');
    panel.className = 'coach-notifications-popover';
    panel.innerHTML = '<div class="coach-notifications-head"><strong>Notifications</strong></div>'
        + '<div class="coach-notifications-list"><p>Chargement...</p></div>';
    topbar.appendChild(panel);

    bell.addEventListener('click', async event => {
        event.stopPropagation();
        panel.classList.toggle('open');
        if (!panel.classList.contains('open')) return;
        const list = panel.querySelector('.coach-notifications-list');
        try {
            const response = await fetch('/encadreur/notifications', {
                headers: {'X-Requested-With': 'XMLHttpRequest'}
            });
            if (!response.ok) throw new Error('notifications');
            const notifications = await response.json();
            list.innerHTML = notifications.length
                ? notifications.map(item => `<article><strong>${escapeHtml(item.objet)}</strong>`
                    + `<span>${escapeHtml(item.message)}</span>`
                    + `<small>${formatDate(item.date)}</small></article>`).join('')
                : '<p>Aucune nouvelle notification.</p>';
        } catch (error) {
            list.innerHTML = '<p>Les notifications sont momentanément indisponibles.</p>';
        }
    });

    document.addEventListener('click', event => {
        if (!panel.contains(event.target) && !bell.contains(event.target)) panel.classList.remove('open');
    });

    function escapeHtml(value) {
        const node = document.createElement('div');
        node.textContent = value || '';
        return node.innerHTML;
    }

    function formatDate(value) {
        if (!value) return '';
        return new Intl.DateTimeFormat('fr-FR', {dateStyle: 'short', timeStyle: 'short'})
            .format(new Date(value));
    }
});
