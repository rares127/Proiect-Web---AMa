class AMAApp {
    constructor() {
        this.currentUser = null;
        this.currentPage = 'auth';
        this.apiBaseUrl = '/ama/api';
        this.cache = {
            abbreviations: null,
            lastFetch: null,
            cacheDuration: 5 * 60 * 1000 //5min
        };
        this.init();
    }

    init() {
        console.log('AMA App initializing...');

        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.setupApp();
            });
        } else {
            this.setupApp();
        }
    }

    setupApp() {
        console.log('Setting up AMA App...');
        this.bindEvents();
        this.initializeComponents();
    }

    bindEvents() {
        console.log('Binding navigation events...');

        // Navigation event
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const page = e.target.getAttribute('data-page');
                console.log('Navigation clicked:', page);
                this.navigateTo(page);
            });
        });

        // Logout/Login event
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async () => {
                if (this.currentUser && this.currentUser.isGuest) {
                    console.log('Guest login clicked');
                    this.goToLogin();
                } else {
                    console.log('Logout clicked');
                    await this.logout();
                }
            });
        }

        this.initializeModal();
    }

    initializeModal() {
        const modal = document.getElementById('abbreviationModal');
        const closeBtn = document.getElementById('modalClose');
        const cancelBtn = document.getElementById('cancelBtn');

        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }

        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }

        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal();
                }
            });
        }
    }

    showAuthenticatedState() {
        console.log('Showing authenticated state for user:', this.currentUser);

        const authPage = document.getElementById('authPage');
        if (authPage) {
            authPage.classList.remove('active');
        }

        const mainNav = document.getElementById('mainNav');
        const userMenu = document.getElementById('userMenu');

        if (mainNav) {
            mainNav.classList.add('show');
        }

        if (userMenu) {
            userMenu.classList.add('show');
        }

        const userInfo = document.getElementById('userInfo');
        if (userInfo && this.currentUser) {
            if (this.currentUser.isGuest) {
                userInfo.textContent = '👤 Vizitator';
                userInfo.style.color = '#888888';
            } else {
                userInfo.textContent = `Bună, ${this.currentUser.username}!`;
                userInfo.style.color = '';
            }
        }

        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn && this.currentUser) {
            if (this.currentUser.isGuest) {
                logoutBtn.textContent = 'Conectare';
            } else {
                logoutBtn.textContent = 'Deconectare';
            }
        }

        this.updateNavigationForGuest();

        setTimeout(() => {
            this.navigateTo('dashboard');
        }, 100);
    }

    updateNavigationForGuest() {
        const navItems = document.querySelectorAll('.nav-item');

        if (this.currentUser && this.currentUser.isGuest) {
            navItems.forEach(item => {
                const page = item.getAttribute('data-page');
                if (page !== 'dashboard') {
                    item.classList.add('nav-disabled');
                }
            });
        } else {
            navItems.forEach(item => {
                item.classList.remove('nav-disabled');
            });
        }
    }

    showUnauthenticatedState() {
        console.log('Showing unauthenticated state');

        const authPage = document.getElementById('authPage');
        if (authPage) {
            authPage.classList.add('active');
        }

        const mainNav = document.getElementById('mainNav');
        const userMenu = document.getElementById('userMenu');

        if (mainNav) {
            mainNav.classList.remove('show');
        }

        if (userMenu) {
            userMenu.classList.remove('show');
        }

        document.querySelectorAll('.page:not(#authPage)').forEach(page => {
            page.classList.remove('active');
        });

        console.log('Unauthenticated state set');
    }

    navigateTo(pageName) {
        console.log('Navigating to page:', pageName);

        if (!this.currentUser && pageName !== 'auth') {
            console.log('User not authenticated, redirecting to auth');
            this.showUnauthenticatedState();
            return;
        }

        if (this.currentUser && this.currentUser.isGuest && pageName !== 'dashboard') {
            console.log('Guest user trying to access restricted page, redirecting to auth');
            this.logout();
            return;
        }

        this.currentPage = pageName;

        document.querySelectorAll('.page').forEach(page => {
            page.classList.remove('active');
        });

        const targetPage = document.getElementById(`${pageName}Page`);
        if (targetPage) {
            targetPage.classList.add('active');
            console.log(`Page ${pageName} activated`);
        }

        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.remove('active');
        });

        const activeNavItem = document.querySelector(`[data-page="${pageName}"]`);
        if (activeNavItem) {
            activeNavItem.classList.add('active');
        }

        this.initializePage(pageName);
    }

    initializePage(pageName) {
        console.log('Initializing page:', pageName);

        setTimeout(() => {
            switch (pageName) {
                case 'dashboard':
                    if (window.Dashboard && window.Dashboard.init) {
                        window.Dashboard.init();
                    }
                    break;
                case 'search':
                    if (window.Search && window.Search.init) {
                        window.Search.init();
                    }
                    break;
                case 'manage':
                    if (window.Abbreviations && window.Abbreviations.init) {
                        window.Abbreviations.init();
                    }
                    break;
            }
        }, 50);
    }

    initializeComponents() {
        console.log('Initializing components...');

        if (window.Auth && window.Auth.init) {
            window.Auth.init();
        }
        if (window.AbbreviationManager && window.AbbreviationManager.init) {
            window.AbbreviationManager.init();
        }
        if (window.AbbreviationModal && window.AbbreviationModal.init) {
            window.AbbreviationModal.init();
        }
        if (window.ModalManager && window.ModalManager.init) {
            window.ModalManager.init();
        }
        if (window.Export && window.Export.init) {
            window.Export.init();
        }
    }

    login(userData, token) {
        console.log('Login successful for user:', userData);

        this.currentUser = userData;
        this.invalidateCache();
        this.showAuthenticatedState();

        setTimeout(() => {
            if (this.currentPage === 'dashboard' && window.Dashboard) {
                window.Dashboard.refresh();
            }
        }, 200);
    }

    goToLogin() {
        console.log('Going to login page');
        this.currentUser = null;
        this.invalidateCache();
        this.showUnauthenticatedState();

        if (window.Dashboard) {
            window.Dashboard.isInitialized = false;
        }
    }

    async logout() {
        console.log('Logging out user');

        if (this.currentUser && !this.currentUser.isGuest && window.Auth) {
            await window.Auth.logout();
        }

        this.currentUser = null;
        this.invalidateCache();
        this.showUnauthenticatedState();

        if (window.Dashboard) {
            window.Dashboard.isInitialized = false;
        }
    }

    isGuest() {
        return this.currentUser && this.currentUser.isGuest;
    }

    closeModal() {
        const modal = document.getElementById('abbreviationModal');
        if (modal) {
            modal.classList.remove('active');
        }
    }

    async getSharedData() {
        // verifica cache inainte de fetch-uri
        const now = Date.now();
        if (this.cache.abbreviations &&
            this.cache.lastFetch &&
            (now - this.cache.lastFetch) < this.cache.cacheDuration) {
            console.log('Returning cached data');
            return this.cache.abbreviations;
        }

        try {
            console.log('Loading fresh data from backend...');
            await this.loadDataFromBackend();
            return this.cache.abbreviations || [];
        } catch (error) {
            console.error('Error loading data from backend:', error);

            if (this.cache.abbreviations) {
                console.log('Using stale cache due to error');
                return this.cache.abbreviations;
            }

            this.cache.abbreviations = [];
            return this.cache.abbreviations;
        }
    }

    async loadDataFromBackend() {
        try {
            const response = await this.authenticatedFetch(`${this.apiBaseUrl}/abbreviations`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (data.success && data.abbreviations) {
                this.cache.abbreviations = data.abbreviations.map(abbrev => ({
                    id: abbrev.id,
                    abbrev: abbrev.name,
                    name: abbrev.name,
                    meanings: abbrev.meanings || [],
                    language: abbrev.language?.code || 'en',
                    domain: abbrev.domain?.code || 'general',
                    description: abbrev.description,
                    author: abbrev.author?.username || 'Unknown',
                    date: this.formatBackendDate(abbrev.createdAt),
                    views: abbrev.views || 0,
                    likes: abbrev.likes || 0,
                    likedBy: abbrev.isLiked ? [this.currentUser?.username].filter(Boolean) : [],
                    favoritedBy: abbrev.isFavorited ? [this.currentUser?.username].filter(Boolean) : []
                }));

                this.cache.lastFetch = Date.now();

                console.log('Data loaded from backend:', this.cache.abbreviations.length, 'abbreviations');
            } else {
                throw new Error('Invalid API response format');
            }
        } catch (error) {
            console.error('Backend loading failed', error);
        }
    }

    formatBackendDate(dateString) {
        if (!dateString) return new Date().toISOString().split('T')[0];

        try {
            const date = new Date(dateString);
            return date.toISOString().split('T')[0]; // Format: YYYY-MM-DD
        } catch (error) {
            return new Date().toISOString().split('T')[0];
        }
    }

    // invalideaza cache pt a forta reincarcarea datelor
    invalidateCache() {
        console.log('Cache invalidated');
        this.cache.abbreviations = null;
        this.cache.lastFetch = null;
    }

    async authenticatedFetch(url, options = {}) {
        const token = localStorage.getItem('ama_token');

        const defaultHeaders = {
            'Content-Type': 'application/json'
        };

        if (token && token !== 'guest_token') {
            defaultHeaders['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            },
            credentials: 'include'
        };

        return fetch(url, config);
    }
}

// intializam applicatia
window.AMA = new AMAApp();

window.AMAUtils = {
    isGuest: () => window.AMA.isGuest(),
};