window.Search = {
    isInitialized: false,
    searchTimeout: null,
    apiBaseUrl: '/ama/api/abbreviations',
    languagesCache: null,
    domainsCache: null,

    async init() {
        if (this.isInitialized) return;

        await this.loadLanguagesAndDomains();

        this.bindEvents();
        this.showWelcomeState();
        this.isInitialized = true;
    },

    async loadLanguagesAndDomains() {
        try {
            console.log('Loading languages and domains from backend...');

            await this.loadLanguages();
            await this.loadDomains();

        } catch (error) {
            console.error('Error loading languages and domains:', error);
        }
    },

    async loadLanguages() {
        try {
            const response = await window.AMA.authenticatedFetch('/ama/api/languages');

            if (response.ok) {
                const data = await response.json();
                if (data.success && data.languages) {
                    this.languagesCache = data.languages;
                    this.populateLanguageFilter(data.languages);
                    console.log('Loaded languages from backend:', data.languages.length);
                } else {
                    throw new Error('Invalid response format for languages');
                }
            } else {
                throw new Error(`Languages API failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error('Failed to load languages from backend:', error);
        }
    },

    async loadDomains() {
        try {
            const response = await window.AMA.authenticatedFetch('/ama/api/domains');

            if (response.ok) {
                const data = await response.json();
                if (data.success && data.domains) {
                    this.domainsCache = data.domains;
                    this.populateDomainFilter(data.domains);
                    console.log('Loaded domains from backend:', data.domains.length);
                } else {
                    throw new Error('Invalid response format for domains');
                }
            } else {
                throw new Error(`Domains API failed with status: ${response.status}`);
            }
        } catch (error) {
            console.error('Failed to load domains from backend:', error);
        }
    },

    populateLanguageFilter(languages) {
        const languageFilter = document.getElementById('languageFilter');
        if (!languageFilter) return;

        // pastrez opt curenta selectata
        const currentValue = languageFilter.value;

        //  add optiunea implicita cand sterg filtrul
        languageFilter.innerHTML = '<option value="">Toate limbile</option>';

        /// add limbile din back
        languages.forEach(language => {
            const option = document.createElement('option');
            option.value = language.code;
            option.textContent = language.name;
            languageFilter.appendChild(option);
        });

        if (currentValue) {
            languageFilter.value = currentValue;
        }
    },

    populateDomainFilter(domains) {
        const domainFilter = document.getElementById('domainFilter');
        if (!domainFilter) return;
        //pastrez opt curenta selectata
        const currentValue = domainFilter.value;

        //  add optiunea implicita cand sterg filtrul
        domainFilter.innerHTML = '<option value="">Toate domeniile</option>';
        // add back
        domains.forEach(domain => {
            const option = document.createElement('option');
            option.value = domain.code;
            option.textContent = domain.name;
            domainFilter.appendChild(option);
        });

        if (currentValue) {
            domainFilter.value = currentValue;
        }
    },

    bindEvents() {
        const searchInput = document.getElementById('searchInput');
        const languageFilter = document.getElementById('languageFilter');
        const domainFilter = document.getElementById('domainFilter');
        const clearBtn = document.getElementById('clearSearch');

        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.debounceSearch();
            });

            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    this.performSearch();
                }
            });
        }

        if (languageFilter) {
            languageFilter.addEventListener('change', () => {
                this.performSearch();
            });
        }

        if (domainFilter) {
            domainFilter.addEventListener('change', () => {
                this.performSearch();
            });
        }

        if (clearBtn) {
            clearBtn.addEventListener('click', () => {
                this.clearSearch();
            });
        }
    },

    //metoda pentru a intarzia cautarea cu 500ms
    debounceSearch() {
        if (this.searchTimeout) {
            clearTimeout(this.searchTimeout);
        }

        this.searchTimeout = setTimeout(() => {
            this.performSearch();
        }, 500);
    },

    async performSearch() {
        const searchTerm = document.getElementById('searchInput')?.value?.trim() || '';
        const languageCode = document.getElementById('languageFilter')?.value || '';
        const domainCode = document.getElementById('domainFilter')?.value || '';

        //nu avem criterii de cautare -> afis welcome state
        if (!searchTerm && !languageCode && !domainCode) {
            this.showWelcomeState();
            return;
        }
        this.showLoadingState();

        try {
            let results = [];
            results = await this.searchBackend(searchTerm, languageCode, domainCode);
            this.displayResults(results, searchTerm, languageCode, domainCode);

        } catch (error) {
            console.error('Search error:', error);
            this.showErrorState();
        }
    },

    async searchBackend(searchTerm, languageCode, domainCode) {
        try {
            // fac URL-ul cu parametri de cautare pentru SearchServlet ca back-ul sa primeasca date corecte
            const params = new URLSearchParams();
            if (searchTerm) params.append('q', searchTerm);
            if (languageCode) params.append('language', languageCode);
            if (domainCode) params.append('domain', domainCode);
            params.append('limit', '50'); //max 50 de afisari (pe toate -> in gestionare)
            params.append('sort', 'alphabetical');

            const url = `/ama/api/search?${params.toString()}`;

            const response = await window.AMA.authenticatedFetch(url);

            if (response.ok) {
                const data = await response.json();
                console.log('Backend search response:', data); // Debug
                return data.success ? data.results : [];
            } else {
                console.error('Backend search failed with status:', response.status);
                throw new Error(`Backend search failed: ${response.status}`);
            }

        } catch (error) {
            console.error('Backend search error:', error);
        }
    },

    displayResults(results, searchTerm, languageCode, domainCode) {
        const container = document.getElementById('searchResults');
        if (!container) {
            console.error('Search results container not found!');
            return;
        }

        if (results.length === 0) {
            this.showNoResults(searchTerm, languageCode, domainCode);
            return;
        }

        // sortam rezultatele, prima data cele care incep cu litera cautata
        if (searchTerm) {
            results.sort((a, b) => {
                const aName = (a.name || a.abbrev || '').toLowerCase();
                const bName = (b.name || b.abbrev || '').toLowerCase();
                const searchLower = searchTerm.toLowerCase();

                const aStarts = aName.startsWith(searchLower);
                const bStarts = bName.startsWith(searchLower);

                if (aStarts && !bStarts) return -1;
                if (!aStarts && bStarts) return 1;

                return aName.localeCompare(bName);
            });
        }

        const resultsHTML = results.map(abbrev => this.renderSearchResult(abbrev)).join('');

        container.innerHTML = `
            <div class="search-header">
                <h3>Rezultate căutare</h3>
                <p>S-au gasit ${results.length} rezultat${results.length !== 1 ? 'e' : ''}</p>
            </div>
            <div class="search-results-grid">
                ${resultsHTML}
            </div>
        `;
    },

    renderSearchResult(abbrev) {
        const currentUser = window.AMA.currentUser;

        // Pentru datele din back folosim numele campurilor
        const abbreviationName = abbrev.name || abbrev.abbrev;
        const authorName = abbrev.author?.username || abbrev.author;
        const languageCode = abbrev.language?.code || abbrev.language;
        const domainCode = abbrev.domain?.code || abbrev.domain;
        const createdDate = this.formatDate(abbrev.createdAt || abbrev.date);

        const isOwner = currentUser && authorName === currentUser.username;

        const viewButton = `<button class="view-btn" onclick="openAbbreviationModal(${abbrev.id})" title="Vezi detalii">
                                <span>👁️</span> Vezi
                            </button>`;

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
                        ${viewButton}
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

    showWelcomeState() {
        const container = document.getElementById('searchResults');
        if (container) {
            container.innerHTML = `
                <div class="search-welcome">
                    <div style="font-size: 4em; margin-bottom: 20px; opacity: 0.7;">🔍</div>
                    <h3>Caută abrevieri</h3>
                    <p>Introdu un termen în câmpul de căutare sau folosește filtrele pentru a găsi abrevierile dorite.</p>
                </div>
            `;
        }
    },

    showLoadingState() {
        const container = document.getElementById('searchResults');
        if (container) {
            container.innerHTML = `
                <div class="loading">
                    <div class="spinner"></div>
                    <p>Se caută...</p>
                </div>
            `;
        }
    },

    showErrorState() {
        const container = document.getElementById('searchResults');
        if (container) {
            container.innerHTML = `
                <div class="no-results">
                    <div class="no-results-icon">⚠️</div>
                    <div class="no-results-title">Eroare la căutare</div>
                    <p>A apărut o eroare în timpul căutării. Te rugăm să încerci din nou.</p>
                    <div class="no-results-suggestion">
                        <button class="btn btn-primary" onclick="Search.performSearch()">
                            Încearcă din nou
                        </button>
                    </div>
                </div>
            `;
        }
    },

    showNoResults(searchTerm, languageCode, domainCode) {
        const container = document.getElementById('searchResults');
        if (container) {
            let searchCriteria = [];
            if (searchTerm) searchCriteria.push(`"${searchTerm}"`);
            if (languageCode) searchCriteria.push(`limba ${this.getLanguageName(languageCode)}`);
            if (domainCode) searchCriteria.push(`domeniul ${this.getDomainDisplayName(domainCode)}`);

            const criteriaText = searchCriteria.length > 0
                ? ` pentru ${searchCriteria.join(', ')}`
                : '';

            container.innerHTML = `
                <div class="no-results">
                    <div class="no-results-title">Nu am găsit rezultate</div>
                    <p>Nu există abrevieri care să corespundă criteriilor${criteriaText}.</p>
                </div>
            `;
        }
    },

    clearSearch() {
        const searchInput = document.getElementById('searchInput');
        const languageFilter = document.getElementById('languageFilter');
        const domainFilter = document.getElementById('domainFilter');

        if (searchInput) searchInput.value = '';
        if (languageFilter) languageFilter.value = '';
        if (domainFilter) domainFilter.value = '';

        this.showWelcomeState();
    },

    // metode Helper
    getDomainDisplayName(domain) {
        // incearca sa gaseasca din cache
        if (this.domainsCache) {
            const domainObj = this.domainsCache.find(d => d.code === domain);
            if (domainObj) return domainObj.name;
        }
        return domains[domain] || domain || 'N/A';
    },

    getLanguageName(code) {
        // incearca sa gaseasca din cache
        if (this.languagesCache) {
            const languageObj = this.languagesCache.find(l => l.code === code);
            if (languageObj) return languageObj.name;
        }
        return languages[code] || code?.toUpperCase() || 'N/A';
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
};

// Auto-initialize
document.addEventListener('DOMContentLoaded', () => {
});