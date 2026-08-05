document.addEventListener('DOMContentLoaded', () => {
    const page = document.querySelector('[data-profile-page]');
    if (!page) return;
    window.lucide?.createIcons();

    requestAnimationFrame(() => {
        document.body.classList.remove('profile-loading');
    });

    const showToast = message => {
        const toast = document.querySelector('[data-profile-toast]');
        if (!toast) return;
        toast.textContent = message;
        toast.classList.add('open');
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => toast.classList.remove('open'), 3200);
    };

    document.querySelector('[data-edit-profile]')?.addEventListener('click', () => {
        const section = document.getElementById('personalInformation');
        section?.scrollIntoView({behavior: 'smooth', block: 'start'});
        window.setTimeout(() => section?.querySelector('input')?.focus(), 450);
    });
    document.querySelector('[data-download-profile]')?.addEventListener('click', () => window.print());

    const animateCounters = () => {
        document.querySelectorAll('.animated-number').forEach(element => {
            const target = Number(element.dataset.value || 0);
            const duration = 850;
            const startTime = performance.now();
            const tick = time => {
                const progress = Math.min(1, (time - startTime) / duration);
                element.textContent = Math.round(target * (1 - Math.pow(1 - progress, 3)));
                if (progress < 1) requestAnimationFrame(tick);
            };
            requestAnimationFrame(tick);
        });
    };
    const observer = new IntersectionObserver(entries => {
        if (!entries.some(entry => entry.isIntersecting)) return;
        animateCounters();
        observer.disconnect();
    }, {threshold: .2});
    observer.observe(page);

    const photoInput = document.getElementById('profilePhotoInput');
    const photoForm = document.querySelector('[data-photo-form]');
    const photoCard = photoForm?.closest('.profile-photo-card');
    const photoSubmit = photoForm?.querySelector('.profile-photo-submit');
    document.querySelector('[data-select-photo]')?.addEventListener('click', () => photoInput?.click());
    photoInput?.addEventListener('change', () => {
        const file = photoInput.files?.[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) {
            showToast('Veuillez sélectionner une image valide.');
            photoInput.value = '';
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showToast('La photo ne doit pas dépasser 5 Mo.');
            photoInput.value = '';
            return;
        }
        const previewBox = document.querySelector('.profile-photo-preview');
        let preview = previewBox?.querySelector('img');
        if (!preview && previewBox) {
            preview = document.createElement('img');
            preview.alt = 'Nouvelle photo de profil';
            previewBox.querySelector('[data-photo-fallback]')?.remove();
            previewBox.prepend(preview);
        }
        const url = URL.createObjectURL(file);
        preview.src = url;
        preview.onload = () => URL.revokeObjectURL(url);
        photoSubmit.hidden = false;
        showToast('Photo prête à être enregistrée.');
    });
    photoForm?.addEventListener('submit', () => photoCard?.classList.add('uploading'));

    const preferenceRoot = document.querySelector('[data-profile-preferences]');
    const preferenceKey = 'dta-stagiaire-profile-preferences';
    let preferences = {};
    try {
        preferences = JSON.parse(localStorage.getItem(preferenceKey) || '{}');
    } catch (error) {
        preferences = {};
    }
    preferenceRoot?.querySelectorAll('[data-preference]').forEach(input => {
        const key = input.dataset.preference;
        if (Object.prototype.hasOwnProperty.call(preferences, key)) input.checked = Boolean(preferences[key]);
        input.addEventListener('change', () => {
            preferences[key] = input.checked;
            localStorage.setItem(preferenceKey, JSON.stringify(preferences));
            document.body.classList.toggle('profile-dark-preview', key === 'dark' && input.checked);
            showToast('Préférence enregistrée.');
        });
        if (key === 'dark' && input.checked) document.body.classList.add('profile-dark-preview');
    });

    const languageSelect = document.querySelector('[data-profile-language]');
    const language = localStorage.getItem('dta-workspace-language') || 'fr';
    if (languageSelect) languageSelect.value = language;
    languageSelect?.addEventListener('change', () => {
        const desired = languageSelect.value;
        const current = localStorage.getItem('dta-workspace-language') || 'fr';
        if (desired !== current) document.querySelector('.workspace-language-button')?.click();
        showToast(desired === 'en' ? 'Language updated.' : 'Langue mise à jour.');
    });

    const passwordModal = document.getElementById('passwordModal');
    const closePassword = () => {
        if (passwordModal) passwordModal.hidden = true;
    };
    document.querySelector('[data-open-password]')?.addEventListener('click', () => {
        passwordModal.hidden = false;
        passwordModal.querySelector('input')?.focus();
    });
    document.querySelectorAll('[data-close-password]').forEach(button =>
        button.addEventListener('click', closePassword));
    passwordModal?.addEventListener('click', event => {
        if (event.target === passwordModal) closePassword();
    });
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') closePassword();
    });
    document.querySelector('[data-security-info]')?.addEventListener('click', () => {
        showToast('La double authentification sera disponible après configuration du service OTP.');
    });

    const userAgent = navigator.userAgent;
    const device = /Mobi|Android/i.test(userAgent) ? 'Appareil mobile'
        : (/Tablet|iPad/i.test(userAgent) ? 'Tablette' : 'Ordinateur');
    const browser = userAgent.includes('Edg/') ? 'Microsoft Edge'
        : (userAgent.includes('Firefox/') ? 'Mozilla Firefox'
            : (userAgent.includes('Chrome/') ? 'Google Chrome'
                : (userAgent.includes('Safari/') ? 'Safari' : 'Navigateur web')));
    const deviceTarget = document.querySelector('[data-device]');
    const browserTarget = document.querySelector('[data-browser]');
    if (deviceTarget) deviceTarget.textContent = device;
    if (browserTarget) browserTarget.textContent = browser;

    const relativeFormatter = new Intl.RelativeTimeFormat('fr', {numeric: 'auto'});
    document.querySelectorAll('.profile-activity-timeline time[data-date]').forEach(element => {
        const date = new Date(element.dataset.date);
        if (Number.isNaN(date.getTime())) return;
        const seconds = Math.round((date.getTime() - Date.now()) / 1000);
        const divisions = [
            {amount: 60, unit: 'second'},
            {amount: 60, unit: 'minute'},
            {amount: 24, unit: 'hour'},
            {amount: 7, unit: 'day'},
            {amount: 4.34524, unit: 'week'},
            {amount: 12, unit: 'month'},
            {amount: Number.POSITIVE_INFINITY, unit: 'year'}
        ];
        let duration = seconds;
        for (const division of divisions) {
            if (Math.abs(duration) < division.amount) {
                element.textContent = relativeFormatter.format(Math.round(duration), division.unit);
                break;
            }
            duration /= division.amount;
        }
    });
});
