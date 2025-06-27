window.AbbreviationManager = {
    isInitialized: false,
    editingAbbreviation: null,
    apiBaseUrl: '/ama/api/abbreviations',
    languagesCache: null,
    domainsCache: null,
    _updating: false, // pt a preveni request-urile duplicate
    _submitting: false, // pt a preveni submit-urile duplicate

    async init() {
        if (this.isInitialized) return;

        await this.loadLanguagesAndDomains();

        this.bindEvents();
        this.preventDoubleSubmit();
        this.isInitialized = true;
    },

    async loadLanguagesAndDomains() {
        try {
            console.log('Loading languages and domains for form...');
            await this.loadLanguages();
            await this.loadDomains();

        } catch (error) {
            console.error('Error loading languages and domains for form:', error);
        }
    },

    async loadLanguages() {
        try {
            const response = await window.AMA.authenticatedFetch('/ama/api/languages');

            if (response.ok) {
                const data = await response.json();
                if (data.success && data.languages) {
                    this.languagesCache = data.languages;
                    this.populateFormLanguages(data.languages);
                    console.log('Loaded languages for form from backend:', data.languages.length);
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
                    this.populateFormDomains(data.domains);
                    console.log('Loaded domains for form from backend:', data.domains.length);
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

    populateFormLanguages(languages) {
        const languageSelect = document.querySelector('#abbreviationForm select[name="language"]');
        if (!languageSelect) return;
        // pastrez optiunea curenta selectata
        const currentValue = languageSelect.value;
        languageSelect.innerHTML = '<option value="">Selectează limba</option>';

        // add limbile din back
        languages.forEach(language => {
            const option = document.createElement('option');
            option.value = language.code;
            option.textContent = language.name;
            languageSelect.appendChild(option);
        });

        if (currentValue) {
            languageSelect.value = currentValue;
        }
    },

    populateFormDomains(domains) {
        const domainSelect = document.querySelector('#abbreviationForm select[name="domain"]');
        if (!domainSelect) return;

        const currentValue = domainSelect.value;
        domainSelect.innerHTML = '<option value="">Selectează domeniul</option>';

        domains.forEach(domain => {
            const option = document.createElement('option');
            option.value = domain.code;
            option.textContent = domain.name;
            domainSelect.appendChild(option);
        });

        if (currentValue) {
            domainSelect.value = currentValue;
        }
    },

    bindEvents() {
        // formul pt add/edit
        const form = document.getElementById('abbreviationForm');
        if (form) {
            // Elimin event listener-ii existenti pt a preveni duplicate
            const existingHandler = form._submitHandler;
            if (existingHandler) {
                form.removeEventListener('submit', existingHandler);
            }

            const newHandler = (e) => {
                e.preventDefault();
                console.log('Form submit handler called');
                this.handleFormSubmit(e);
            };

            form._submitHandler = newHandler;
            form.addEventListener('submit', newHandler);
        }

        const addBtn = document.getElementById('addAbbreviationBtn');
        if (addBtn) {
            addBtn.addEventListener('click', () => {
                this.openAddModal();
            });
        }

        const cancelBtn = document.getElementById('cancelBtn');
        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }

        const closeBtn = document.getElementById('modalClose');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                this.closeModal();
            });
        }
    },

    preventDoubleSubmit() {
        const form = document.getElementById('abbreviationForm');
        if (form) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                let clickCount = 0;

                if (submitBtn._clickHandler) {
                    submitBtn.removeEventListener('click', submitBtn._clickHandler);
                }

                const clickHandler = (e) => {
                    clickCount++;
                    console.log('Submit button click count:', clickCount);

                    if (clickCount > 1) {
                        e.preventDefault();
                        e.stopPropagation();
                        console.log('Prevented double-click submit');
                        return false;
                    }

                    setTimeout(() => {
                        clickCount = 0;
                    }, 2000);
                };

                submitBtn._clickHandler = clickHandler;
                submitBtn.addEventListener('click', clickHandler);
            }
        }
    },

    // verifc permisiunile pt abrevierea selectata
    checkPermissions(abbreviation) {
        const currentUser = window.AMA?.currentUser;
        if (!currentUser) return { canEdit: false, canDelete: false };

        const isAdmin = currentUser.role === 'admin';
        const isOwner = abbreviation.author?.username === currentUser.username;

        return {
            canEdit: isAdmin || isOwner,
            canDelete: isAdmin || isOwner,
            isAdmin: isAdmin,
            isOwner: isOwner
        };
    },

    async openAddModal() {
        this.editingAbbreviation = null;

        const modalTitle = document.getElementById('modalTitle');
        const form = document.getElementById('abbreviationForm');

        if (modalTitle) {
            modalTitle.textContent = 'Adaugă Abreviere Nouă';
        }

        if (form) {
            form.reset();
        }

        await this.ensureFormDataLoaded();
        window.ModalManager.openModal('abbreviationModal', {
            onClose: () => {
                this.editingAbbreviation = null;
                this._submitting = false; // Reset flag
                this._updating = false; // Reset flag
            }
        });
    },

    async editAbbreviation(abbreviationId) {
        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (!data.success) {
                console.error('Nu s-a putut încărca abrevierea:', data.message);
                alert(data.message || 'Nu s-a putut încărca abrevierea');
                return;
            }
            const abbreviation = data.abbreviation;

            const permissions = this.checkPermissions(abbreviation);
            if (!permissions.canEdit) {
                alert('Nu aveți permisiunea să editați această abreviere!');
                return;
            }

            this.editingAbbreviation = abbreviation;

            const modalTitle = document.getElementById('modalTitle');
            const form = document.getElementById('abbreviationForm');

            if (modalTitle) {
                modalTitle.textContent = 'Editează Abreviere';
            }

            if (form) {
                await this.ensureFormDataLoaded();

                form.querySelector('[name="abbreviation"]').value = abbreviation.name;
                form.querySelector('[name="meaning"]').value = abbreviation.meanings.join(', ');
                form.querySelector('[name="language"]').value = abbreviation.language?.code || '';
                form.querySelector('[name="domain"]').value = abbreviation.domain?.code || '';
                form.querySelector('[name="description"]').value = abbreviation.description || '';
            }

            window.ModalManager.openModal('abbreviationModal', {
                onClose: () => {
                    this.editingAbbreviation = null;
                    this._submitting = false; // Reset flag
                    this._updating = false; // Reset flag
                }
            });

        } catch (error) {
            console.error('Eroare la încărcarea abrevierii:', error);
            alert('Eroare la încărcarea abrevierii pentru editare');
        }
    },

    async ensureFormDataLoaded() {
        if (!this.languagesCache || !this.domainsCache) {
            await this.loadLanguagesAndDomains();
        } else {
            const languageSelect = document.querySelector('#abbreviationForm select[name="language"]');
            const domainSelect = document.querySelector('#abbreviationForm select[name="domain"]');

            if (languageSelect && languageSelect.children.length <= 1) {
                this.populateFormLanguages(this.languagesCache);
            }

            if (domainSelect && domainSelect.children.length <= 1) {
                this.populateFormDomains(this.domainsCache);
            }
        }
    },

    async deleteAbbreviation(abbreviationId) {
        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}`);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();

            if (!data.success) {
                console.error('Nu s-a putut încărca abrevierea:', data.message);
                alert(data.message || 'Nu s-a putut încărca abrevierea');
                return;
            }

            const abbreviation = data.abbreviation;

            const permissions = this.checkPermissions(abbreviation);
            if (!permissions.canDelete) {
                alert('Nu aveți permisiunea să ștergeți această abreviere!');
                return;
            }

            const confirmMessage = permissions.isAdmin
                ? `Sigur doriți să ștergeți abrevierea "${abbreviation.name}" creată de ${abbreviation.author?.username}?`
                : `Sigur doriți să ștergeți abrevierea "${abbreviation.name}"?`;

            const confirmed = confirm(confirmMessage);
            if (!confirmed) return;

            const deleteResponse = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}`, {
                method: 'DELETE'
            });

            const deleteData = await deleteResponse.json();

            if (deleteResponse.ok && deleteData.success) {
                alert('Abrevierea a fost ștearsă cu succes!');

                if (window.ModalManager) {
                    window.ModalManager.closeAllModals();
                }

                window.AMA.invalidateCache();
                this.refreshAllPages();
            } else {
                alert(deleteData.message || 'Eroare la ștergerea abrevierii');
            }

        } catch (error) {
            console.error('Eroare la ștergerea abrevierii:', error);
            alert('Eroare la comunicarea cu serverul pentru ștergere');
        }
    },

    async handleFormSubmit(event) {
        console.log('handleFormSubmit called');

        if (this._submitting) {
            console.log('Form submission already in progress, ignoring');
            return;
        }

        this._submitting = true;

        const form = event.target;
        const formData = new FormData(form);

        const abbreviationData = {
            name: formData.get('abbreviation').trim(),
            meanings: formData.get('meaning').split(',').map(m => m.trim()).filter(m => m),
            language: formData.get('language'),
            domain: formData.get('domain'),
            description: formData.get('description').trim() || null
        };

        const validation = await this.validateAbbreviation(abbreviationData);
        if (!validation.isValid) {
            alert(validation.message);
            this._submitting = false;
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');
        if (!submitBtn) {
            console.error('Submit button not found');
            this._submitting = false;
            return;
        }

        const originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Se salvează...';

        try {
            if (this.editingAbbreviation) {
                console.log('Calling updateAbbreviation for ID:', this.editingAbbreviation.id);
                await this.updateAbbreviation(this.editingAbbreviation.id, abbreviationData);
            } else {
                console.log('Calling createAbbreviation');
                await this.createAbbreviation(abbreviationData);
            }
        } catch (error) {
            console.error('Form submission error:', error);
            alert('Eroare la salvarea abrevierii!');
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = originalText;
            this._submitting = false;
            console.log('Form submission completed');
        }
    },

    async validateAbbreviation(data) {
        console.log('Validating data:', data); // Debug

        if (!data || typeof data !== 'object') {
            return { isValid: false, message: 'Date invalide!' };
        }

        if (!data.name || typeof data.name !== 'string' || data.name.trim().length === 0) {
            return { isValid: false, message: 'Abrevierea este obligatorie!' };
        }

        const trimmedName = data.name.trim();

        if (trimmedName.length < 1) {
            return { isValid: false, message: 'Abrevierea este obligatorie!' };
        }

        if (trimmedName.length > 10) {
            return { isValid: false, message: 'Abrevierea nu poate depăși 10 caractere!' };
        }

        if (!data.meanings || !Array.isArray(data.meanings) || data.meanings.length === 0) {
            return { isValid: false, message: 'Cel puțin o semnificație este obligatorie!' };
        }

        const validMeanings = data.meanings.filter(meaning =>
            meaning && typeof meaning === 'string' && meaning.trim().length > 0
        );

        if (validMeanings.length === 0) {
            return { isValid: false, message: 'Semnificatia este obligatorie!' };
        }

        if (!data.language || typeof data.language !== 'string' || data.language.trim().length === 0) {
            return { isValid: false, message: 'Limba este obligatorie!' };
        }

        if (!data.domain || typeof data.domain !== 'string' || data.domain.trim().length === 0) {
            return { isValid: false, message: 'Domeniul este obligatoriu!' };
        }

        // verific daca aceeasi abreviere exista deja in combinatia de limba si domeniu
        if (!this.editingAbbreviation) {
            try {
                const sharedData = await window.AMA.getSharedData();
                if (Array.isArray(sharedData)) {
                    const existing = sharedData.find(abbrev => {
                        const nameMatches = (abbrev.abbrev || abbrev.name || '').toLowerCase() === trimmedName.toLowerCase();

                        const languageMatches = (abbrev.language === data.language || abbrev.language?.code === data.language);

                        const domainMatches = (abbrev.domain === data.domain || abbrev.domain?.code === data.domain);

                        return nameMatches && languageMatches && domainMatches;
                    });

                    if (existing) {
                        return {
                            isValid: false,
                            message: `Abrevierea "${trimmedName}" există deja în combinația ${data.language}/${data.domain}!\n\nPoți crea aceeași abreviere în altă limbă sau alt domeniu.`
                        };
                    }
                }
            } catch (error) {
                console.warn('Could not check for existing abbreviations:', error);
            }
        }

        console.log('Validation passed'); // Debug
        return { isValid: true };
    },

    async createAbbreviation(data) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            alert('Trebuie să fiți autentificat!');
            return;
        }

        try {
            const response = await window.AMA.authenticatedFetch(this.apiBaseUrl, {
                method: 'POST',
                body: JSON.stringify(data)
            });

            const responseData = await response.json();

            if (response.ok && responseData.success) {
                alert('Abrevierea a fost adăugată cu succes!');
                this.closeModal();

                window.AMA.invalidateCache();
                this.refreshAllPages();
            } else {
                alert(responseData.message || 'Eroare la adăugarea abrevierii');
            }

        } catch (error) {
            console.error('Eroare la crearea abrevierii:', error);
            alert('Eroare la comunicarea cu serverul');
        }
    },

    async updateAbbreviation(id, data) {
        if (this._updating) {
            console.log('Update already in progress, ignoring duplicate request');
            return;
        }

        this._updating = true;
        console.log('Starting update for abbreviation ID:', id);

        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });

            const responseData = await response.json();

            if (response.ok && responseData.success) {
                console.log('Update successful');
                alert('Abrevierea a fost actualizată cu succes!');
                this.closeModal();

                window.AMA.invalidateCache();
                this.refreshAllPages();

                if (window.AbbreviationModal && window.AbbreviationModal.isOpen() &&
                    window.AbbreviationModal.currentAbbreviation?.id === id) {
                    window.AbbreviationModal.open(id);
                }
            } else {
                alert(responseData.message || 'Eroare la actualizarea abrevierii');
            }

        } catch (error) {
            console.error('Eroare la actualizarea abrevierii:', error);
            alert('Eroare la comunicarea cu serverul');
        } finally {
            this._updating = false;
            console.log('Update process completed');
        }
    },

    async toggleLike(abbreviationId) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            alert('Trebuie să fiți autentificat pentru a aprecia!');
            return false;
        }

        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}/like`, {
                method: 'POST'
            });

            const data = await response.json();

            if (response.ok && data.success) {
                return data.liked;
            } else {
                console.error('Eroare la apreciere:', data.message);
                return false;
            }

        } catch (error) {
            console.error('Eroare la toggle like:', error);
            return false;
        }
    },

    async toggleFavorite(abbreviationId) {
        const currentUser = window.AMA.currentUser;
        if (!currentUser || currentUser.isGuest) {
            alert('Trebuie să fiți autentificat pentru a adăuga la favorite!');
            return false;
        }

        try {
            const response = await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}/favorite`, {
                method: 'POST'
            });

            const data = await response.json();

            if (response.ok && data.success) {
                return data.favorited;
            } else {
                console.error('Eroare la favorite:', data.message);
                return false;
            }

        } catch (error) {
            console.error('Eroare la toggle favorite:', error);
            return false;
        }
    },

    async incrementViews(abbreviationId) {
        try {
            await window.AMA.authenticatedFetch(`${this.apiBaseUrl}/${abbreviationId}/view`, {
                method: 'POST'
            });
        } catch (error) {
            console.error('Eroare la incrementarea vizualizărilor:', error);
        }
    },

    closeModal() {
        if (window.ModalManager) {
            window.ModalManager.closeModal('abbreviationModal');
        }
        this.editingAbbreviation = null;
        this._submitting = false;
        this._updating = false;
    },

    refreshAllPages() {
        console.log('Refreshing all pages...');

        if (window.Dashboard && window.Dashboard.isInitialized) {
            window.Dashboard.loadPopularAbbreviations();
            window.Dashboard.loadStatistics();
        }

        if (window.Search && window.Search.isInitialized) {
            window.Search.performSearch();
        }

        if (window.Abbreviations && window.Abbreviations.isInitialized) {
            if (window.Abbreviations.refresh) {
                window.Abbreviations.refresh();
            } else if (window.Abbreviations.applyCurrentFilter) {
                window.Abbreviations.applyCurrentFilter();
            }
        }
    },

    getPermissionsForAbbreviation(abbreviation) {
        return this.checkPermissions(abbreviation);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    window.AbbreviationManager.init();
});