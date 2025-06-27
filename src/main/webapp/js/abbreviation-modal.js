window.AbbreviationModal = {
    isInitialized: false,
    currentAbbreviation: null,
    apiBaseUrl: '/ama/api/abbreviations',

    init() {
        if (this.isInitialized) return;

        this.createModal();
        this.bindEvents();
        this.isInitialized = true;
    },

    createModal() {
        if (document.getElementById('abbreviationDetailModal')) {
            return;
        }

        const modalHTML = `
            <div id="abbreviationDetailModal" class="abbreviation-detail-modal">
                <div class="modal-backdrop" onclick="AbbreviationModal.close()"></div>
                <div class="abbreviation-detail-content">
                    <div class="modal-detail-header">
                        <button class="modal-detail-close" onclick="AbbreviationModal.close()">&times;</button>
                        <h1 class="abbreviation-title-large" id="modalAbbrevTitle"></h1>
                        <p class="abbreviation-subtitle" id="modalAbbrevSubtitle"></p>
                    </div>
                    
                    <div class="modal-detail-body">
                        <div class="meanings-section">
                            <h3>📝 Semnificații</h3>
                            <div class="meanings-grid" id="modalMeanings"></div>
                        </div>

                        <div class="details-grid">
                            <div class="detail-card">
                                <span class="detail-icon">🌍</span>
                                <div class="detail-label">Limbă</div>
                                <div class="detail-value" id="modalLanguage"></div>
                            </div>
                            <div class="detail-card">
                                <span class="detail-icon">🏷️</span>
                                <div class="detail-label">Domeniu</div>
                                <div class="detail-value" id="modalDomain"></div>
                            </div>
                            <div class="detail-card">
                                <span class="detail-icon">📅</span>
                                <div class="detail-label">Data adăugării</div>
                                <div class="detail-value" id="modalDate"></div>
                            </div>
                            <div class="detail-card">
                                <span class="detail-icon">👤</span>
                                <div class="detail-label">Autor</div>
                                <div class="detail-value" id="modalAuthor"></div>
                            </div>
                        </div>

                        <div class="stats-section">
                            <div class="stat-item">
                                <span class="stat-icon">👁️</span>
                                <span class="stat-number" id="modalViews">0</span>
                                <span class="stat-label">Vizualizări</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-icon">❤️</span>
                                <span class="stat-number" id="modalLikes">0</span>
                                <span class="stat-label">Aprecieri</span>
                            </div>
                            <div class="stat-item">
                                <span class="stat-icon">⭐</span>
                                <span class="stat-number" id="modalFavorites">0</span>
                                <span class="stat-label">Favorite</span>
                            </div>
                        </div>

                        <div class="description-section" id="modalDescriptionSection" style="display: none;">
                            <h3>📖 Descriere</h3>
                            <p class="description-text" id="modalDescription"></p>
                        </div>
                    </div>

                    <div class="modal-detail-actions">
                        <div class="modal-action-buttons" id="modalActionButtons">
                        </div>
                        <div id="modalDropdown">
                        </div>
                    </div>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', modalHTML);
    },

    bindEvents() {
        const modalContent = document.querySelector('.abbreviation-detail-content');
        if (modalContent) {
            modalContent.addEventListener('click', (e) => {
                e.stopPropagation();
            });
        }

        document.addEventListener('click', (e) => {
            if (!e.target.closest('.dropdown')) {
                this.closeAllDropdowns();
            }
        });

        document.addEventListener('keydown', (e) => {
            // ESC pt inchidere dropdown
            if (e.key === 'Escape') {
                this.closeAllDropdowns();
            }
        });
    },

    async open(abbreviationId) {
        try {
            this.showLoadingModal();

            window.ModalManager.openModal('abbreviationDetailModal', {
                onClose: () => {
                    this.currentAbbreviation = null;
                }
            });

            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (data.success) {
                this.currentAbbreviation = data.abbreviation;

                await window.AbbreviationManager.incrementViews(abbreviationId);

                this.populateModal(data.abbreviation);
            } else {
                throw new Error(data.message || 'Abrevierea nu a fost găsită');
            }

        } catch (error) {
            console.error('Eroare la încărcarea abrevierii:', error);
            this.showErrorModal(error.message);
        }
    },

    showLoadingModal() {
        document.getElementById('modalAbbrevTitle').textContent = 'Se încarcă...';
        document.getElementById('modalAbbrevSubtitle').textContent = '';

        const bodyElement = document.querySelector('.modal-detail-body');
        if (bodyElement) {
            bodyElement.innerHTML = `
                <div style="text-align: center; padding: 40px;">
                    <div class="spinner" style="width: 40px; height: 40px; margin: 0 auto 20px;"></div>
                    <p>Se încarcă detaliile abrevierii...</p>
                </div>
            `;
        }
    },

    showErrorModal(errorMessage) {
        document.getElementById('modalAbbrevTitle').textContent = 'Eroare';
        document.getElementById('modalAbbrevSubtitle').textContent = '';

        const bodyElement = document.querySelector('.modal-detail-body');
        if (bodyElement) {
            bodyElement.innerHTML = `
                <div style="text-align: center; padding: 40px;">
                    <div style="font-size: 3em; margin-bottom: 20px;">❌</div>
                    <h3>Nu s-a putut încărca abrevierea</h3>
                    <p>${errorMessage}</p>
                    <button class="btn btn-primary" onclick="AbbreviationModal.close()">Închide</button>
                </div>
            `;
        }
    },

    close() {
        this.closeAllDropdowns();
        window.ModalManager.closeModal('abbreviationDetailModal');
    },

    isOpen() {
        return window.ModalManager.isModalOpen('abbreviationDetailModal');
    },

    populateModal(abbreviation) {
        const currentUser = window.AMA.currentUser;

        this.restoreModalStructure();

        document.getElementById('modalAbbrevTitle').textContent = abbreviation.name;
        document.getElementById('modalAbbrevSubtitle').textContent =
            `${abbreviation.meanings?.length || 0} semnificație${(abbreviation.meanings?.length || 0) !== 1 ? 'ți' : ''}`;

        // Semnificatie
        const meaningsContainer = document.getElementById('modalMeanings');
        if (abbreviation.meanings && abbreviation.meanings.length > 0) {
            meaningsContainer.innerHTML = abbreviation.meanings
                .map(meaning => `<div class="meaning-item">${meaning}</div>`)
                .join('');
        } else {
            meaningsContainer.innerHTML = '<div class="meaning-item">Nu sunt disponibile semnificații</div>';
        }

        // Detalii
        document.getElementById('modalLanguage').textContent =
            abbreviation.language?.name || this.getLanguageName(abbreviation.language?.code) || 'N/A';
        document.getElementById('modalDomain').textContent =
            abbreviation.domain?.name || this.getDomainName(abbreviation.domain?.code) || 'N/A';
        document.getElementById('modalDate').textContent = this.formatDate(abbreviation.createdAt);
        document.getElementById('modalAuthor').textContent = abbreviation.author?.username || 'Unknown';

        // Statistici
        document.getElementById('modalViews').textContent = abbreviation.views || 0;
        document.getElementById('modalLikes').textContent = abbreviation.likes || 0;
        document.getElementById('modalFavorites').textContent = abbreviation.favorites || 0;

        // Descriere
        const descriptionSection = document.getElementById('modalDescriptionSection');
        const descriptionText = document.getElementById('modalDescription');
        if (abbreviation.description) {
            descriptionText.textContent = abbreviation.description;
            descriptionSection.style.display = 'block';
        } else {
            descriptionSection.style.display = 'none';
        }

        this.renderActionButtons(abbreviation);
    },

    restoreModalStructure() {
        const bodyElement = document.querySelector('.modal-detail-body');
        if (!bodyElement) return;

        if (!bodyElement.querySelector('.meanings-section')) {
            bodyElement.innerHTML = `
                <div class="meanings-section">
                    <h3>📝 Semnificații</h3>
                    <div class="meanings-grid" id="modalMeanings"></div>
                </div>

                <div class="details-grid">
                    <div class="detail-card">
                        <span class="detail-icon">🌍</span>
                        <div class="detail-label">Limbă</div>
                        <div class="detail-value" id="modalLanguage"></div>
                    </div>
                    <div class="detail-card">
                        <span class="detail-icon">🏷️</span>
                        <div class="detail-label">Domeniu</div>
                        <div class="detail-value" id="modalDomain"></div>
                    </div>
                    <div class="detail-card">
                        <span class="detail-icon">📅</span>
                        <div class="detail-label">Data adăugării</div>
                        <div class="detail-value" id="modalDate"></div>
                    </div>
                    <div class="detail-card">
                        <span class="detail-icon">👤</span>
                        <div class="detail-label">Autor</div>
                        <div class="detail-value" id="modalAuthor"></div>
                    </div>
                </div>

                <div class="stats-section">
                    <div class="stat-item">
                        <span class="stat-icon">👁️</span>
                        <span class="stat-number" id="modalViews">0</span>
                        <span class="stat-label">Vizualizări</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-icon">❤️</span>
                        <span class="stat-number" id="modalLikes">0</span>
                        <span class="stat-label">Aprecieri</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-icon">⭐</span>
                        <span class="stat-number" id="modalFavorites">0</span>
                        <span class="stat-label">Favorite</span>
                    </div>
                </div>

                <div class="description-section" id="modalDescriptionSection" style="display: none;">
                    <h3>📖 Descriere</h3>
                    <p class="description-text" id="modalDescription"></p>
                </div>
            `;
        }
    },

    renderActionButtons(abbreviation) {
        const currentUser = window.AMA.currentUser;
        const actionButtonsContainer = document.getElementById('modalActionButtons');
        const dropdownContainer = document.getElementById('modalDropdown');

        // guest
        if (!currentUser || currentUser.isGuest) {
            actionButtonsContainer.innerHTML = `
                <p style="color: var(--text-secondary); font-size: 0.9em;">
                    Conectează-te pentru a aprecia sau adăuga la favorite
                </p>
            `;
            dropdownContainer.innerHTML = '';
            return;
        }

        // autentificati
        const hasLiked = abbreviation.isLiked || false;
        const isFavorite = abbreviation.isFavorited || false;

        actionButtonsContainer.innerHTML = `
            <button class="btn-like ${hasLiked ? 'btn-liked' : ''}" 
                    onclick="AbbreviationModal.toggleLike(${abbreviation.id})" 
                    id="modalLikeBtn">
                <span class="icon">${hasLiked ? '❤️' : '🤍'}</span>
                ${hasLiked ? 'Apreciat' : 'Apreciază'}
            </button>
            <button class="btn-favorite ${isFavorite ? 'btn-favorited' : ''}" 
                    onclick="AbbreviationModal.toggleFavorite(${abbreviation.id})"
                    id="modalFavoriteBtn">
                <span class="icon">${isFavorite ? '⭐' : '☆'}</span>
                ${isFavorite ? 'Favorit' : 'Adaugă la favorite'}
            </button>
        `;

        const permissions = window.AbbreviationManager?.getPermissionsForAbbreviation(abbreviation) ||
            { canEdit: false, canDelete: false, isAdmin: false };

        const dropdownHTML = `
            <div class="dropdown">
                <button class="dropdown-toggle" onclick="AbbreviationModal.toggleDropdown(this)" title="Opțiuni">
                    ⋮
                </button>
                <div class="dropdown-menu">
                    <button class="dropdown-item" onclick="window.Export.exportAbbreviation(${abbreviation.id}, 'markdown')">
                        📝 Export Markdown
                    </button>
                    <button class="dropdown-item" onclick="window.Export.exportAbbreviation(${abbreviation.id}, 'html')">
                        🌐 Export HTML
                    </button>
                    ${(permissions.canEdit || permissions.canDelete) ? '<div class="dropdown-divider"></div>' : ''}
                    ${permissions.canEdit ? `
                        <button class="dropdown-item" onclick="AbbreviationModal.editAbbreviation(${abbreviation.id})">
                            ✏️ Editează
                        </button>
                    ` : ''}
                    ${permissions.canDelete ? `
                        <button class="dropdown-item danger" onclick="AbbreviationModal.deleteAbbreviation(${abbreviation.id})">
                            🗑️ Șterge
                        </button>
                    ` : ''}
                </div>
            </div>
        `;
        dropdownContainer.innerHTML = dropdownHTML;
    },

    toggleDropdown(buttonElement) {
        const dropdownElement = buttonElement.closest('.dropdown');
        if (!dropdownElement) return;

        const isActive = dropdownElement.classList.contains('active');

        this.closeAllDropdowns();

        if (!isActive) {
            dropdownElement.classList.add('active');

            const firstItem = dropdownElement.querySelector('.dropdown-item');
            if (firstItem) {
                setTimeout(() => firstItem.focus(), 100);
            }
        }
    },

    closeAllDropdowns() {
        const openDropdowns = document.querySelectorAll('.dropdown.active');
        openDropdowns.forEach(dropdown => {
            dropdown.classList.remove('active');
        });
    },

    editAbbreviation(abbreviationId) {
        if (!window.AbbreviationManager) {
            console.error('Modulul de gestionare nu este disponibil!');
            return;
        }
        this.closeAllDropdowns();
        // se deschide modalul de editare din abbrevatiaon-manager.js
        window.AbbreviationManager.editAbbreviation(abbreviationId);
    },

    async deleteAbbreviation(abbreviationId) {
        if (!window.AbbreviationManager) {
            console.error('Modulul de gestionare nu este disponibil!');
            return;
        }
        this.closeAllDropdowns();

        await window.AbbreviationManager.deleteAbbreviation(abbreviationId);

        this.close();
    },

    async toggleLike(id) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            alert('Trebuie să fii autentificat pentru a da like!');
            return;
        }

        const likeBtn = document.getElementById('modalLikeBtn');
        if (likeBtn) {
            likeBtn.disabled = true;
        }

        try {
            const liked = await window.AbbreviationManager.toggleLike(id);

            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${id}`);

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    this.currentAbbreviation = data.abbreviation;
                    this.populateModal(data.abbreviation);
                }
            }

        } catch (error) {
            console.error('Eroare la toggle like:', error);
            alert('Eroare la apreciere');
        } finally {
            if (likeBtn) {
                likeBtn.disabled = false;
            }
        }
    },

    async toggleFavorite(id) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            alert('Trebuie să fii autentificat pentru a adăuga la favorite!');
            return;
        }

        const favoriteBtn = document.getElementById('modalFavoriteBtn');
        if (favoriteBtn) {
            favoriteBtn.disabled = true;
        }

        try {
            const favorited = await window.AbbreviationManager.toggleFavorite(id);

            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${id}`);

            if (response.ok) {
                const data = await response.json();
                if (data.success) {
                    this.currentAbbreviation = data.abbreviation;
                    this.populateModal(data.abbreviation);
                }
            }

        } catch (error) {
            console.error('Eroare la toggle favorite:', error);
            alert('Eroare la adăugarea la favorite');
        } finally {
            if (favoriteBtn) {
                favoriteBtn.disabled = false;
            }
        }
    },

    getLanguageName(code) {
        if (!code) return 'N/A';

        const languages = {
            'ro': 'Română',
            'en': 'Engleză',
            'fr': 'Franceză',
            'de': 'Germană',
            'es': 'Spaniolă',
            'it': 'Italiană'
        };
        return languages[code] || code.toUpperCase();
    },

    getDomainName(code) {
        if (!code) return 'N/A';

        const domains = {
            'medical': 'Medical',
            'tech': 'Tehnologie',
            'business': 'Business',
            'science': 'Științe',
            'general': 'General',
            'education': 'Educație',
            'legal': 'Juridic'
        };
        return domains[code] || code;
    },

    formatDate(dateString) {
        if (!dateString) return 'N/A';

        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('ro-RO', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            });
        } catch (error) {
            return 'N/A';
        }
    }
};

window.openAbbreviationModal = function(abbreviationId) {
    if (!window.AbbreviationModal.isInitialized) {
        window.AbbreviationModal.init();
    }
    window.AbbreviationModal.open(abbreviationId);
};

document.addEventListener('DOMContentLoaded', () => {
    window.AbbreviationModal.init();
});