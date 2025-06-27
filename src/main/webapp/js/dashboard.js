window.Dashboard = {
    isInitialized: false,
    apiBaseUrl: '/ama/api/abbreviations',

    init() {
        if (this.isInitialized) return;

        this.loadDashboardData();
        this.isInitialized = true;
    },

    async loadDashboardData() {
        try {
            this.showLoadingState();
            await this.delay(800);
            await this.loadStatistics();
            await this.loadPopularAbbreviations();
        } catch (error) {
            console.error('Dashboard loading error:', error);
        }
    },

    async loadStatistics() {
        try {
            const response = await fetch(`${this.apiBaseUrl}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            const data = await response.json();

            if (data.success) {
                const stats = {
                    total: data.abbreviations.length,
                    languages: this.countUniqueLanguages(data.abbreviations),
                    domains: this.countUniqueDomains(data.abbreviations),
                    userContributions: this.countUserContributions(data.abbreviations)
                };

                this.displayStatistics(stats);
            } else {
            }

        } catch (error) {
            console.error('Error loading statistics:', error);
        }
    },

    async loadPopularAbbreviations() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/popular?limit=8`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });

            const data = await response.json();

            if (data.success) {
                this.displayPopularAbbreviations(data.abbreviations);
            } else {
                console.error('Failed to load popular abbreviations:', data.message);
            }

        } catch (error) {
            console.error('Error loading popular abbreviations:', error);
        }
    },

    displayStatistics(stats) {
        const totalElement = document.getElementById('totalAbbreviations');
        const languagesElement = document.getElementById('totalLanguages');
        const domainsElement = document.getElementById('totalDomains');
        const contributionsElement = document.getElementById('userContributions');

        if (totalElement) {
            this.animateNumber(totalElement, 0, stats.total, 1000);
        }

        if (languagesElement) {
            this.animateNumber(languagesElement, 0, stats.languages, 1200);
        }

        if (domainsElement) {
            this.animateNumber(domainsElement, 0, stats.domains, 1400);
        }

        if (contributionsElement) {
            const userContributions = window.AMA && window.AMA.isGuest() ? 0 : stats.userContributions;
            this.animateNumber(contributionsElement, 0, userContributions, 1600);
        }
    },

    displayPopularAbbreviations(abbreviations) {
        const container = document.getElementById('popularAbbreviations');

        if (container && abbreviations.length > 0) {
            container.innerHTML = `
                <div class="card" style="grid-column: 1 / -1;">
                    <h3>🏆 Abrevieri Populare</h3>
                    <div class="popular-list">
                        ${abbreviations.slice(0, 8).map((abbrev, index) => this.renderPopularItem(abbrev, index + 1)).join('')}
                    </div>
                </div>
            `;
        }
    },

    renderPopularItem(abbrev, rank) {
        const currentUser = window.AMA.currentUser;
        const isGuest = window.AMA && window.AMA.isGuest();
        const isOwner = currentUser && abbrev.author && abbrev.author.username === currentUser.username;

        // guest are restrictii la detalii
        const viewButton = isGuest
            ? `<button class="view-btn guest-blurred" onclick="AMA.goToLogin()" title="Conectează-te pentru a vedea detalii">
                <span>🔒</span> Vezi
           </button>`
            : `<button class="view-btn" onclick="openAbbreviationModal(${abbrev.id})" title="Vezi detalii">
                <span>👁️</span> Vezi
           </button>`;

        return `
        <div class="popular-item">
            <div class="popular-rank">${rank}</div>
            <div class="popular-content">
                <div class="popular-title">${abbrev.name}</div>
                <div class="meta-tags" style="margin-top: 8px;">
                    <span class="meta-tag language">${abbrev.language?.code?.toUpperCase() || 'N/A'}</span>
                    <span class="meta-tag domain">${abbrev.domain?.code || abbrev.domain?.name || 'N/A'}</span>
                    <span class="meta-tag date">${this.formatDate(abbrev.createdAt)}</span>
                    <span class="meta-tag author">👤 ${abbrev.author?.username || 'Unknown'}</span>
                    ${(isOwner && !isGuest) ? '<span class="meta-tag" style="background: #48bb78;">Propria</span>' : ''}
                </div>
            </div>
            <div class="popular-stats">
                <div class="compact-stats">
                    <span class="stat-compact">👁️ ${abbrev.views || 0}</span>
                    <span class="stat-compact">❤️ ${abbrev.likes || 0}</span>
                    <span class="stat-compact">⭐ ${abbrev.favorites || 0}</span>
                </div>
                ${viewButton}
            </div>
        </div>
    `;
    },

    countUniqueLanguages(abbreviations) {
        const languages = new Set();
        abbreviations.forEach(abbrev => {
            if (abbrev.language && abbrev.language.code) {
                languages.add(abbrev.language.code);
            }
        });
        return languages.size;
    },

    countUniqueDomains(abbreviations) {
        const domains = new Set();
        abbreviations.forEach(abbrev => {
            if (abbrev.domain && abbrev.domain.code) {
                domains.add(abbrev.domain.code);
            }
        });
        return domains.size;
    },

    countUserContributions(abbreviations) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            return 0;
        }

        return abbreviations.filter(abbrev =>
            abbrev.author && abbrev.author.username === currentUser.username
        ).length;
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

    animateNumber(element, start, end, duration) {
        if (!element) return;

        const startTime = performance.now();
        const difference = end - start;

        const step = (currentTime) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            const easeOutCubic = 1 - Math.pow(1 - progress, 3);
            const current = Math.floor(start + difference * easeOutCubic);

            element.textContent = current.toLocaleString();

            if (progress < 1) {
                requestAnimationFrame(step);
            } else {
                element.textContent = end.toLocaleString();
            }
        };

        requestAnimationFrame(step);
    },

    showLoadingState() {
        const statsElements = [
            'totalAbbreviations',
            'totalLanguages',
            'totalDomains',
            'userContributions'
        ];

        statsElements.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.innerHTML = '<div class="spinner" style="width: 20px; height: 20px; margin: 0 auto;"></div>';
            }
        });

        const popularContainer = document.getElementById('popularAbbreviations');
        if (popularContainer) {
            popularContainer.innerHTML = `
                <div class="card" style="grid-column: 1 / -1;">
                    <div class="loading">
                        <div class="spinner"></div>
                        <p>Se încarcă abrevierile populare...</p>
                    </div>
                </div>
            `;
        }
    },

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    },

    async refresh() {
        await this.loadDashboardData();
    }
};