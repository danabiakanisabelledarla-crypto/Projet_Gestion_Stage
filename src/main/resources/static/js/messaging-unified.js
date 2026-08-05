document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('.unified-messaging');
    if (!root || root.dataset.messagingReady === 'true') return;
    root.dataset.messagingReady = 'true';

    const normalize = value => (value || '').normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    const formatTime = value => {
        if (!value) return '';
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? ''
            : new Intl.DateTimeFormat(document.documentElement.lang || 'fr', {
                hour: '2-digit',
                minute: '2-digit'
            }).format(date);
    };
    const formatFileSize = bytes => {
        const size = Number(bytes || 0);
        return size >= 1048576
            ? `${(size / 1048576).toFixed(1)} Mo`
            : `${Math.max(1, Math.round(size / 1024))} Ko`;
    };
    const initials = name => (name || '').split(/\s+/).filter(Boolean)
        .slice(0, 2).map(part => part.charAt(0).toUpperCase()).join('');

    const conversationList = document.getElementById('conversationList');
    const search = document.getElementById('conversationSearch');
    const filterButton = document.getElementById('conversationFilterButton');
    const filterMenu = document.getElementById('conversationFilterMenu');
    const discussion = root.querySelector('.um-discussion[data-conversation-id]');
    const conversationId = Number(discussion?.dataset.conversationId || 0);
    const messageBase = root.dataset.messageBase || window.location.pathname;
    let activeRole = 'ALL';
    let synchronizing = false;

    const getConversations = () => [...document.querySelectorAll('.um-conversation')];
    const applyConversationFilters = () => {
        const query = normalize(search?.value);
        getConversations().forEach(item => {
            const matchesSearch = !query || normalize(item.dataset.search).includes(query);
            const matchesRole = activeRole === 'ALL' || item.dataset.role === activeRole;
            item.hidden = !(matchesSearch && matchesRole);
        });
    };
    search?.addEventListener('input', applyConversationFilters);
    filterButton?.addEventListener('click', event => {
        event.stopPropagation();
        filterMenu.hidden = !filterMenu.hidden;
        filterButton.classList.toggle('active', !filterMenu.hidden);
        filterButton.setAttribute('aria-expanded', String(!filterMenu.hidden));
    });
    filterMenu?.querySelectorAll('[data-role-filter]').forEach(button =>
        button.addEventListener('click', () => {
            activeRole = button.dataset.roleFilter;
            filterMenu.querySelectorAll('button')
                .forEach(item => item.classList.toggle('active', item === button));
            filterMenu.hidden = true;
            filterButton.classList.remove('active');
            filterButton.setAttribute('aria-expanded', 'false');
            applyConversationFilters();
        }));
    document.addEventListener('click', event => {
        if (filterMenu && !filterMenu.hidden && !event.target.closest('.um-filter-wrap')) {
            filterMenu.hidden = true;
            filterButton?.classList.remove('active');
            filterButton?.setAttribute('aria-expanded', 'false');
        }
    });

    const messageBox = document.getElementById('discussionMessages');
    if (messageBox) messageBox.scrollTop = messageBox.scrollHeight;
    document.querySelectorAll('.file-size').forEach(label => {
        label.textContent = formatFileSize(label.dataset.size);
    });

    const modal = document.getElementById('contactModal');
    const selectedContactId = document.getElementById('selectedContactId');
    const startButton = document.getElementById('startConversationButton');
    const contactSearch = document.getElementById('contactSearch');
    const contacts = [...document.querySelectorAll('.um-contact')];
    const closeModal = () => {
        if (modal) modal.hidden = true;
    };
    document.querySelectorAll('[data-open-contacts]').forEach(button =>
        button.addEventListener('click', () => {
            if (modal) modal.hidden = false;
            contactSearch?.focus();
        }));
    document.querySelectorAll('[data-close-contacts]')
        .forEach(button => button.addEventListener('click', closeModal));
    modal?.addEventListener('click', event => {
        if (event.target === modal) closeModal();
    });
    contacts.forEach(contact => contact.addEventListener('click', () => {
        contacts.forEach(item => item.classList.toggle('selected', item === contact));
        selectedContactId.value = contact.dataset.id;
        startButton.disabled = false;
    }));
    contactSearch?.addEventListener('input', () => {
        const query = normalize(contactSearch.value);
        contacts.forEach(contact => {
            contact.hidden = Boolean(query) && !normalize(contact.dataset.search).includes(query);
        });
    });

    const content = document.getElementById('messageContent');
    const emojiButton = document.getElementById('emojiButton');
    const emojiPicker = document.getElementById('emojiPicker');
    const closeEmojiPicker = () => {
        if (!emojiPicker) return;
        emojiPicker.hidden = true;
        emojiButton?.setAttribute('aria-expanded', 'false');
    };
    emojiButton?.addEventListener('click', event => {
        event.stopPropagation();
        emojiPicker.hidden = !emojiPicker.hidden;
        emojiButton.setAttribute('aria-expanded', String(!emojiPicker.hidden));
    });
    emojiPicker?.querySelectorAll('button').forEach(button =>
        button.addEventListener('click', () => {
            const start = content.selectionStart ?? content.value.length;
            const end = content.selectionEnd ?? content.value.length;
            content.value = content.value.slice(0, start)
                + button.textContent
                + content.value.slice(end);
            const cursor = start + button.textContent.length;
            content.focus();
            content.setSelectionRange(cursor, cursor);
            closeEmojiPicker();
        }));
    document.addEventListener('click', event => {
        if (!event.target.closest('#emojiPicker, #emojiButton')) closeEmojiPicker();
    });

    const selectedFiles = document.getElementById('selectedFiles');
    const fileInputs = ['attachmentInput', 'imageInput']
        .map(id => document.getElementById(id))
        .filter(Boolean);
    const renderFiles = () => {
        if (!selectedFiles) return;
        selectedFiles.replaceChildren();
        fileInputs.flatMap(input => [...input.files]).forEach(file => {
            const preview = document.createElement('div');
            preview.className = 'um-file-preview';
            if (file.type.startsWith('image/')) {
                const image = document.createElement('img');
                image.src = URL.createObjectURL(file);
                image.onload = () => URL.revokeObjectURL(image.src);
                preview.append(image);
            } else {
                const icon = document.createElement('i');
                icon.className = 'fa-regular fa-file-lines';
                preview.append(icon);
            }
            const name = document.createElement('span');
            name.textContent = file.name;
            preview.append(name);
            selectedFiles.append(preview);
        });
        selectedFiles.hidden = selectedFiles.childElementCount === 0;
    };
    document.querySelectorAll('[data-file-trigger]').forEach(button =>
        button.addEventListener('click', () =>
            document.getElementById(button.dataset.fileTrigger)?.click()));
    fileInputs.forEach(input => input.addEventListener('change', renderFiles));

    content?.addEventListener('input', () => {
        content.style.height = 'auto';
        content.style.height = Math.min(content.scrollHeight, 110) + 'px';
    });
    content?.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            document.getElementById('messageForm')?.requestSubmit();
        }
    });

    const createConversationElement = state => {
        const link = document.createElement('a');
        link.className = 'um-conversation';
        link.dataset.conversationId = state.id;
        link.innerHTML = `
            <span class="um-avatar"></span>
            <span class="um-conversation-copy">
                <strong class="um-conversation-name"></strong>
                <small class="um-conversation-role"></small>
                <small class="um-conversation-preview"></small>
            </span>
            <em class="um-conversation-time"></em>
            <span class="um-unread" hidden></span>`;
        return link;
    };

    const updateConversations = states => {
        if (!conversationList) return;
        states.forEach(state => {
            let link = conversationList.querySelector(
                `.um-conversation[data-conversation-id="${state.id}"]`
            );
            if (!link) {
                link = createConversationElement(state);
            }
            link.href = `${messageBase}?convId=${state.id}`;
            link.dataset.search = normalize(`${state.nom} ${state.email} ${state.dernierMessage}`);
            link.dataset.role = state.role;
            link.classList.toggle('active', Number(state.id) === conversationId);

            const avatar = link.querySelector('.um-avatar');
            avatar.textContent = initials(state.nom);
            avatar.classList.toggle('online', Boolean(state.enLigne));
            avatar.classList.toggle('offline', !state.enLigne);
            link.querySelector('.um-conversation-name').textContent = state.nom;
            link.querySelector('.um-conversation-role').textContent = state.role
                .toLowerCase().replace('_stage', '').replace(/^\w/, value => value.toUpperCase());
            link.querySelector('.um-conversation-preview').textContent =
                state.dernierMessage || 'Nouvelle conversation';
            link.querySelector('.um-conversation-time').textContent = formatTime(state.date);
            const unread = link.querySelector('.um-unread');
            unread.textContent = state.nonLus;
            unread.hidden = !state.nonLus;
            conversationList.append(link);
        });
        applyConversationFilters();
    };

    const createMessageElement = message => {
        const article = document.createElement('article');
        article.className = `um-message ${message.envoye ? 'sent' : 'received'}`;
        article.dataset.messageId = message.id;

        if (!message.envoye) {
            const avatar = document.createElement('span');
            avatar.className = 'um-avatar small';
            avatar.textContent = message.initiales;
            article.append(avatar);
        }

        const bubble = document.createElement('div');
        bubble.className = 'um-bubble';
        if (message.contenu) {
            const paragraph = document.createElement('p');
            paragraph.textContent = message.contenu;
            bubble.append(paragraph);
        }
        if (message.typePieceJointe === 'image') {
            const link = document.createElement('a');
            link.className = 'um-image-message';
            link.href = message.apercuUrl;
            link.target = '_blank';
            const image = document.createElement('img');
            image.src = message.apercuUrl;
            image.alt = message.nomFichier || 'Image';
            link.append(image);
            bubble.append(link);
        } else if (message.typePieceJointe === 'fichier') {
            const link = document.createElement('a');
            link.className = 'um-file-message';
            link.href = message.telechargementUrl;
            const icon = document.createElement('i');
            icon.className = 'fa-regular fa-file-lines';
            const details = document.createElement('span');
            const name = document.createElement('strong');
            name.textContent = message.nomFichier || 'Document';
            const size = document.createElement('small');
            size.textContent = formatFileSize(message.tailleFichier);
            details.append(name, size);
            const download = document.createElement('i');
            download.className = 'fa-solid fa-download';
            link.append(icon, details, download);
            bubble.append(link);
        }
        const time = document.createElement('time');
        const timeLabel = document.createElement('span');
        timeLabel.textContent = formatTime(message.date);
        time.append(timeLabel);
        if (message.envoye) {
            const seen = document.createElement('i');
            seen.className = `fa-solid fa-check-double um-seen${message.lu ? ' read' : ''}`;
            seen.title = message.lu ? 'Vu' : 'Envoyé';
            time.append(seen);
        }
        bubble.append(time);
        article.append(bubble);
        return article;
    };

    const updateSeenIndicators = ids => {
        (ids || []).forEach(id => {
            const indicator = messageBox?.querySelector(
                `[data-message-id="${id}"] .um-seen`
            );
            indicator?.classList.add('read');
            if (indicator) indicator.title = 'Vu';
        });
    };

    const updatePresence = online => {
        const status = root.querySelector('[data-contact-status]');
        if (!status) return;
        status.classList.toggle('online', Boolean(online));
        status.classList.toggle('offline', !online);
        const label = status.querySelector('span');
        if (label) label.textContent = online ? 'En ligne' : 'Hors ligne';
    };

    const synchronize = async (forceScroll = false) => {
        if (!conversationId || !messageBox || synchronizing || document.hidden) return;
        synchronizing = true;
        try {
            const after = Number(messageBox.dataset.lastMessageId || 0);
            const response = await fetch(
                `/messagerie/actualiser?convId=${conversationId}&after=${after}`,
                {headers: {'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest'}}
            );
            if (!response.ok) return;
            const state = await response.json();
            const nearBottom = messageBox.scrollHeight - messageBox.scrollTop
                - messageBox.clientHeight < 100;
            (state.messages || []).forEach(message => {
                if (messageBox.querySelector(`[data-message-id="${message.id}"]`)) return;
                messageBox.append(createMessageElement(message));
            });
            messageBox.dataset.lastMessageId = state.dernierMessageId || after;
            updateSeenIndicators(state.messagesLus);
            updatePresence(state.contactEnLigne);
            updateConversations(state.conversations || []);
            if (forceScroll || nearBottom || (state.messages || []).length) {
                messageBox.scrollTop = messageBox.scrollHeight;
            }
        } catch (error) {
            // A temporary network failure must not interrupt message composition.
        } finally {
            synchronizing = false;
        }
    };

    const messageForm = document.getElementById('messageForm');
    messageForm?.addEventListener('submit', async event => {
        event.preventDefault();
        const hasText = Boolean(content?.value.trim());
        const hasFiles = fileInputs.some(input => input.files.length > 0);
        if (!hasText && !hasFiles) return;
        const sendButton = messageForm.querySelector('.um-send');
        sendButton.disabled = true;
        try {
            const response = await fetch(messageForm.action, {
                method: 'POST',
                body: new FormData(messageForm)
            });
            if (!response.ok) throw new Error('send');
            content.value = '';
            content.style.height = '';
            fileInputs.forEach(input => { input.value = ''; });
            renderFiles();
            closeEmojiPicker();
            await synchronize(true);
        } catch (error) {
            messageForm.submit();
        } finally {
            sendButton.disabled = false;
        }
    });

    if (conversationId) {
        synchronize(false);
        window.setInterval(() => synchronize(false), 2500);
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) synchronize(true);
        });
    }
});
