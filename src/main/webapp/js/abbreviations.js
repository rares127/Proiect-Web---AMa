// Manage Module
window.Abbreviations = {
    isInitialized: false,
    currentFilter: null,
    hasFiltered: false,
    apiBaseUrl: '/ama/api/abbreviations',

    init() {
        if (this.isInitialized) return;

        this.bindEvents();
        this.showInitialState();
        this.isInitialized = true;
    },

    bindEvents() {
        // buton de add abreviere
        const addBtn = document.getElementById('addAbbreviationBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => {
                if (window.AbbreviationManager) {
                    window.AbbreviationManager.openAddModal();
                }
            });
        }
        const viewMyBtn = document.getElementById('viewMyAbbreviationsBtn');
        const viewFavBtn = document.getElementById('viewFavoritesBtn');
        const viewAllBtn = document.getElementById('viewAllAbbreviationsBtn');
        const clearFilterBtn = document.getElementById('clearManageFilter');

        if (viewMyBtn) {
            viewMyBtn.addEventListener('click', () => {
                console.log('My abbreviations button clicked');
                this.filterAbbreviations('mine');
            });
        }

        if (viewFavBtn) {
            viewFavBtn.addEventListener('click', () => {
                console.log('Favorites button clicked');
                this.filterAbbreviations('favorites');
            });
        }

        if (viewAllBtn) {
            viewAllBtn.addEventListener('click', () => {
                console.log('All abbreviations button clicked');
                this.filterAbbreviations('all');
            });
        }

        if (clearFilterBtn) {
            clearFilterBtn.addEventListener('click', () => {
                this.clearFilter();
            });
        }
    },

    async filterAbbreviations(filter) {
        console.log('Filtering abbreviations:', filter);

        this.currentFilter = filter;
        this.hasFiltered = true;
        this.updateFilterInfo();
        this.updateActiveButton();
        await this.applyCurrentFilter();
    },

    clearFilter() {
        this.currentFilter = null;
        this.hasFiltered = false;
        this.updateFilterInfo();
        this.updateActiveButton();
        this.showInitialState();
    },

    showInitialState() {
        const container = document.getElementById('abbreviationsList');
        if (container) {
            container.innerHTML = `
                <div class="search-welcome" style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: #666;">
                    <h3 style="margin-bottom: 10px; color: #333;">Gestionare Abrevieri</h3>
                    <p>Selectează o opțiune pentru a vedea abrevierile...</p>
                    <p style="font-size: 0.9em; margin-top: 15px;">Propriile abrevieri, favorite sau toate abrevierile</p>
                </div>
            `;
        }
    },

    async applyCurrentFilter() {
        console.log('Applying filter:', this.currentFilter);

        const currentUser = window.AMA?.currentUser;
        if (!currentUser) {
            this.showEmptyState('Nu ești autentificat!');
            return;
        }

        this.showLoadingState();

        try {
            let filteredAbbreviations = [];

            switch (this.currentFilter) {
                case 'mine':
                    console.log('Filtering for user:', currentUser.username);
                    filteredAbbreviations = await this.getUserAbbreviations();
                    break;

                case 'favorites':
                    console.log('Filtering favorites for user:', currentUser.username);
                    filteredAbbreviations = await this.getUserFavorites();
                    break;

                case 'all':
                default:
                    console.log('Showing all abbreviations');
                    filteredAbbreviations = await this.getAllAbbreviations();
                    break;
            }
            filteredAbbreviations.sort((a, b) => a.name.localeCompare(b.name));

            console.log('Final filtered abbreviations:', filteredAbbreviations.length);
            this.displayAbbreviations(filteredAbbreviations);

        } catch (error) {
            console.error('Error applying filter:', error);
            this.showErrorState();
        }
    },

    async getUserAbbreviations() {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            return [];
        }

        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}?filter=mine`);

            if (response.ok) {
                const data = await response.json();
                return data.success ? data.abbreviations : [];
            }
        } catch (error) {
            console.error('Error getting user abbreviations:', error);
        }
    },

    async getUserFavorites() {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            return [];
        }

        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}?filter=favorites`);

            if (response.ok) {
                const data = await response.json();
                return data.success ? data.abbreviations : [];
            }
        } catch (error) {
            console.error('Error getting user favorites:', error);
        }
    },

    async getAllAbbreviations() {
        try {
            const response = await window.AMA.authenticatedFetch(this.apiBaseUrl);

            if (response.ok) {
                const data = await response.json();
                return data.success ? data.abbreviations : [];
            }
        } catch (error) {
            console.error('Error getting all abbreviations:', error);
        }
    },

    displayAbbreviations(abbreviations) {
        const container = document.getElementById('abbreviationsList');
        if (!container) {
            console.error('Abbreviations list container not found!');
            return;
        }

        if (abbreviations.length === 0) {
            console.log('No abbreviations to display, showing empty state');
            container.innerHTML = this.renderEmptyState();
            return;
        }

        console.log('Displaying', abbreviations.length, 'abbreviations');
        container.innerHTML = abbreviations.map(abbrev => this.renderAbbreviation(abbrev)).join('');
    },

    renderAbbreviation(abbrev) {
        const currentUser = window.AMA.currentUser;
        const permissions = window.AbbreviationManager?.getPermissionsForAbbreviation(abbrev) ||
            { canEdit: false, canDelete: false, isAdmin: false };

        const abbreviationName = abbrev.name || abbrev.abbrev;
        const authorName = abbrev.author?.username || abbrev.author;
        const languageCode = abbrev.language?.code || abbrev.language;
        const domainCode = abbrev.domain?.code || abbrev.domain;
        const createdDate = this.formatDate(abbrev.createdAt || abbrev.date);

        const isOwner = currentUser && authorName === currentUser.username;

        return `
            <div class="search-result-card">
                <div class="search-result-header">
                    <div>
                        <h3 class="search-result-title">
                            ${abbreviationName}
                            ${isOwner ? '<span class="propria-tag">Propria</span>' : ''}
                        </h3>
                        <div class="meta-tags">
                            <span class="meta-tag language">${(languageCode || '').toUpperCase()}</span>
                            <span class="meta-tag domain">${this.getDomainDisplayName(domainCode)}</span>
                            <span class="meta-tag date">${createdDate}</span>
                            <span class="meta-tag author">👤 ${authorName}</span>
                        </div>
                    </div>
                    <div class="search-result-actions">
                        <div class="compact-stats">
                            <span class="stat-compact">👁️ ${abbrev.views || 0}</span>
                            <span class="stat-compact">❤️ ${abbrev.likes || 0}</span>
                            <span class="stat-compact">⭐ ${abbrev.favorites || 0}</span>
                        </div>
                        <button class="view-btn" onclick="openAbbreviationModal(${abbrev.id})" title="Vezi detalii">
                            <span>👁️</span> Vezi
                        </button>
                    </div>
                </div>
                
                <div class="search-result-meanings">
                    <strong>Semnificații:</strong>
                    <ul>
                        ${(abbrev.meanings || []).map(meaning =>
            `<li>${meaning}</li>`
        ).join('')}
                    </ul>
                </div>
                
                ${abbrev.description ?
            `<div class="search-result-description">
                        ${abbrev.description.length > 120 ?
                abbrev.description.substring(0, 120) + '...' :
                abbrev.description
            }
                    </div>` :
            ''
        }
            </div>
        `;
    },

    renderEmptyState() {
        let title, description, suggestion;

        switch (this.currentFilter) {
            case 'mine':
                title = 'Nu ai încă abrevieri proprii';
                description = 'Adaugă prima ta abreviere pentru a o vedea aici.';
                suggestion = '<button class="btn btn-primary" onclick="AbbreviationManager.openAddModal()">➕ Adaugă prima abreviere</button>';
                break;

            case 'favorites':
                title = 'Nu ai încă favorite';
                description = 'Navighează prin abrevieri și adaugă-le la favorite pentru a le vedea aici.';
                suggestion = '<button class="btn btn-secondary" onclick="AMA.navigateTo(\'search\')">🔍 Explorează abrevierile</button>';
                break;

            case 'all':
                title = 'Nu există abrevieri';
                description = 'Încă nu există abrevieri în sistem.';
                suggestion = '<button class="btn btn-primary" onclick="AbbreviationManager.openAddModal()">➕ Adaugă prima abreviere</button>';
                break;

            default:
                title = 'Selectează o opțiune';
                description = 'Alege una dintre opțiunile de mai sus pentru a vedea abrevierile.';
                suggestion = '';
                break;
        }

        return `
            <div class="no-results">
                <div class="no-results-icon">📝</div>
                <div class="no-results-title">${title}</div>
                <p>${description}</p>
                ${suggestion ? `<div class="no-results-suggestion">${suggestion}</div>` : ''}
            </div>
        `;
    },

    updateFilterInfo() {
        const filterInfo = document.getElementById('manageFilterInfo');

        if (filterInfo) {
            filterInfo.style.display = 'none';
        }
    },

    updateActiveButton() {
        console.log('Updating active button for filter:', this.currentFilter);

        document.querySelectorAll('.manage-actions .btn').forEach(btn => {
            btn.classList.remove('active');
        });

        if (this.currentFilter) {
            const buttonMap = {
                'all': 'viewAllAbbreviationsBtn',
                'mine': 'viewMyAbbreviationsBtn',
                'favorites': 'viewFavoritesBtn'
            };

            const activeBtn = document.getElementById(buttonMap[this.currentFilter]);
            if (activeBtn) {
                activeBtn.classList.add('active');
                console.log('Activated button:', buttonMap[this.currentFilter]);
            } else {
                console.error('Button not found:', buttonMap[this.currentFilter]);
            }
        }
    },

    showLoadingState() {
        const container = document.getElementById('abbreviationsList');
        if (container) {
            container.innerHTML = `
                <div class="loading" style="grid-column: 1 / -1; text-align: center; padding: 60px 20px; color: #666;">
                    <div class="spinner" style="width: 40px; height: 40px; border: 4px solid #f3f3f3; border-top: 4px solid #667eea; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px;"></div>
                    <p>Se încarcă abrevierile...</p>
                </div>
            `;
        }
    },

    showErrorState() {
        const container = document.getElementById('abbreviationsList');
        if (container) {
            container.innerHTML = `
                <div class="no-results">
                    <div class="no-results-icon">⚠️</div>
                    <div class="no-results-title">Eroare la încărcare</div>
                    <p>A apărut o eroare în timpul încărcării abrevierilor.</p>
                    <div class="no-results-suggestion">
                        <button class="btn btn-primary" onclick="Abbreviations.applyCurrentFilter()">
                            🔄 Încearcă din nou
                        </button>
                    </div>
                </div>
            `;
        }
    },

    showEmptyState(message) {
        const container = document.getElementById('abbreviationsList');
        if (container) {
            container.innerHTML = `
                <div class="no-results">
                    <div class="no-results-icon">❌</div>
                    <div class="no-results-title">${message}</div>
                </div>
            `;
        }
    },

    getDomainDisplayName(domain) {
        const domains = {
            'medical': 'Medical',
            'tech': 'Tehnologie',
            'business': 'Business',
            'science': 'Științe',
            'general': 'General',
            'education': 'Educație',
            'legal': 'Juridic'
        };
        return domains[domain] || domain || 'N/A';
    },

    formatDate(dateString) {
        if (!dateString) return 'N/A';

        try {
            const date = new Date(dateString);
            return date.toLocaleDateString('ro-RO');
        } catch (error) {
            return 'N/A';
        }
    },

    async refresh() {
        console.log('Refreshing abbreviations, current filter:', this.currentFilter);

        window.AMA.invalidateCache();

        if (this.currentFilter) {
            await this.applyCurrentFilter();
        } else {
            this.showInitialState();
        }
    }
};

// Auto-initialize
document.addEventListener('DOMContentLoaded', () => {
});