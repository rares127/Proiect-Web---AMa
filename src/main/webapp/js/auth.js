window.Auth = {
    isInitialized: false,
    apiBaseUrl: '/ama/api/auth',

    init() {
        if (this.isInitialized) return;

        console.log('Auth module initializing...');
        this.bindEvents();
        this.checkExistingSession();
        this.isInitialized = true;
        console.log('Auth module initialized');
    },

    bindEvents() {
        console.log('Auth: Binding events...');

        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                console.log('Login form submitted');
                this.handleLogin(e);
            });
            console.log('Login form event bound');
        } else {
            console.error('Login form not found');
        }

        // guest
        const guestBtn = document.getElementById('continueAsGuestBtn');
        if (guestBtn) {
            guestBtn.addEventListener('click', (e) => {
                e.preventDefault();
                console.log('Continue as guest clicked');
                this.handleGuestLogin();
            });
            console.log('Guest button event bound');
        }

        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRegister(e);
            });
        }

        // de la login la register
        const showRegisterBtn = document.getElementById('showRegisterBtn');
        if (showRegisterBtn) {
            showRegisterBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.showRegisterForm();
            });
        }

        // cand apas enter se da submit la login/register
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && document.getElementById('authPage')?.classList.contains('active')) {
                const activeForm = document.querySelector('.auth-form:not([style*="display: none"])');
                if (activeForm) {
                    const submitBtn = activeForm.querySelector('button[type="submit"]');
                    if (submitBtn && !submitBtn.disabled) {
                        submitBtn.click();
                    }
                }
            }
        });
    },

    // verific daca este deja o sesiune activa cand se incarca pagina
    async checkExistingSession() {
        console.log('Auth: Checking existing session...');

        const token = localStorage.getItem('ama_token');
        if (!token) {
            console.log('Auth: No existing token found');
            return;
        }

        try {
            const response = await fetch(`${this.apiBaseUrl}/me`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                credentials: 'include'
            });

            const data = await response.json();

            if (data.success && data.user) {
                console.log('Auth: Valid session found, auto-login');

                // Auto-login cu datele existente
                if (window.AMA && window.AMA.login) {
                    window.AMA.login(data.user, token);
                }
            } else {
                console.log('Auth: Invalid session, clearing local storage');
                this.clearLocalAuth();
            }

        } catch (error) {
            console.error('Auth: Error checking session:', error);
            this.clearLocalAuth();
        }
    },

    handleGuestLogin() {
        console.log('Auth: Handling guest login...');

        //se creeaza un guest temporar
        const guestUser = {
            id: 'guest',
            username: 'Vizitator',
            email: null,
            role: 'guest',
            isGuest: true
        };

        console.log('Auth: Guest user created:', guestUser);
        console.log('Acces ca vizitator acordat!');

        // notific app.js ca s-a logat un guest
        setTimeout(() => {
            if (window.AMA && window.AMA.login) {
                console.log('Auth: Calling AMA.login for guest');
                window.AMA.login(guestUser, 'guest_token');
            } else {
                console.error('Auth: AMA app not found or login method missing');
            }
        }, 500);
    },

    async handleLogin(event) {
        console.log('Auth: Handling login...');

        const form = event.target;
        const formData = new FormData(form);
        const credentials = {
            username: formData.get('username'),
            password: formData.get('password')
        };

        console.log('Auth: Login credentials:', { username: credentials.username, password: '***' });
        if (!credentials.username || !credentials.password) {
            this.showError('Te rugăm să completezi toate câmpurile.');
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');

        try {
            this.setLoadingState(submitBtn, true);
            this.clearMessages();

            const response = await fetch(`${this.apiBaseUrl}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify(credentials)
            });

            const data = await response.json();

            console.log('Auth: Login response:', data);

            if (data.success) {
                console.log('Autentificare reușită!');

                localStorage.setItem('ama_token', data.token);
                localStorage.setItem('ama_user', JSON.stringify(data.user));

                const userData = {
                    id: data.user.id,
                    username: data.user.username,
                    email: data.user.email,
                    role: data.user.role || 'user',
                    isGuest: false
                };

                console.log('Auth: User data prepared:', userData);

                // notific app.js ca s-a logat un user
                setTimeout(() => {
                    if (window.AMA && window.AMA.login) {
                        console.log('Auth: Calling AMA.login');
                        window.AMA.login(userData, data.token);
                    } else {
                        console.error('Auth: AMA app not found or login method missing');
                    }
                }, 500);

            } else {
                this.showError(data.message || 'Datele de autentificare sunt incorecte.');
            }

        } catch (error) {
            console.error('Auth: Login error:', error);
            this.showError('Eroare la conectarea cu serverul. Te rugăm să încerci din nou.');
        } finally {
            this.setLoadingState(submitBtn, false);
        }
    },

    async handleRegister(event) {
        const form = event.target;
        const formData = new FormData(form);
        const userData = {
            username: formData.get('username'),
            email: formData.get('email'),
            password: formData.get('password'),
            confirmPassword: formData.get('confirmPassword')
        };

        const validation = this.validateRegistration(userData);
        if (!validation.isValid) {
            this.showError(validation.message);
            return;
        }

        const submitBtn = form.querySelector('button[type="submit"]');

        try {
            this.setLoadingState(submitBtn, true);
            this.clearMessages();

            const response = await fetch(`${this.apiBaseUrl}/register`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({
                    username: userData.username,
                    email: userData.email,
                    password: userData.password
                })
            });

            const data = await response.json();

            if (data.success) {
                this.showSuccess('Cont creat cu succes! Te poți autentifica acum.');
                setTimeout(() => {
                    this.showLoginForm();
                }, 2000);
            } else {
                this.showError(data.message || 'Eroare la crearea contului.');
            }

        } catch (error) {
            console.error('Register error:', error);
            this.showError('Eroare la conectarea cu serverul. Te rugăm să încerci din nou.');
        } finally {
            this.setLoadingState(submitBtn, false);
        }
    },

    async logout() {
        try {
            console.log('Auth: Logging out...');

            const response = await fetch(`${this.apiBaseUrl}/logout`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include'
            });
            this.clearLocalAuth();
            console.log('Auth: Logout completed');

        } catch (error) {
            console.error('Auth: Logout error:', error);
            this.clearLocalAuth();
        }
    },

    clearLocalAuth() {
        localStorage.removeItem('ama_token');
        localStorage.removeItem('ama_user');
    },

    validateRegistration(userData) {
        if (!userData.username || userData.username.length < 3) {
            return { isValid: false, message: 'Numele de utilizator trebuie să aibă cel puțin 3 caractere.' };
        }

        if (!userData.email || !this.isValidEmail(userData.email)) {
            return { isValid: false, message: 'Te rugăm să introduci o adresă de email validă.' };
        }

        if (!userData.password || userData.password.length < 6) {
            return { isValid: false, message: 'Parola trebuie să aibă cel puțin 6 caractere.' };
        }

        if (userData.password !== userData.confirmPassword) {
            return { isValid: false, message: 'Parolele nu coincid.' };
        }

        return { isValid: true };
    },

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    },

    showLoginForm() {
        const loginForm = document.getElementById('loginForm');
        const registerForm = document.getElementById('registerForm');
        const authTitle = document.querySelector('.auth-title');

        if (loginForm) loginForm.style.display = 'flex';
        if (registerForm) registerForm.style.display = 'none';
        if (authTitle) authTitle.textContent = 'AMa';

        this.clearMessages();
    },

    showRegisterForm() {
        const loginForm = document.getElementById('loginForm');
        const registerForm = document.getElementById('registerForm');
        const authTitle = document.querySelector('.auth-title');

        if (!registerForm) {
            this.createRegisterForm();
            return;
        }

        if (loginForm) loginForm.style.display = 'none';
        if (registerForm) registerForm.style.display = 'flex';
        if (authTitle) authTitle.textContent = 'Înregistrare';

        this.clearMessages();
    },

    createRegisterForm() {
        const authCard = document.querySelector('.auth-card');
        const loginForm = document.getElementById('loginForm');

        const registerFormHTML = `
            <form id="registerForm" class="auth-form">
                <div class="form-group">
                    <label for="regUsername">Nume utilizator</label>
                    <input type="text" id="regUsername" name="username" required minlength="3">
                </div>
                <div class="form-group">
                    <label for="regEmail">Email</label>
                    <input type="email" id="regEmail" name="email" required>
                </div>
                <div class="form-group">
                    <label for="regPassword">Parolă</label>
                    <input type="password" id="regPassword" name="password" required minlength="6">
                </div>
                <div class="form-group">
                    <label for="regConfirmPassword">Confirmă parola</label>
                    <input type="password" id="regConfirmPassword" name="confirmPassword" required>
                </div>
                <button type="submit" class="btn btn-primary">Înregistrează-te</button>
                <button type="button" class="btn btn-secondary" onclick="Auth.showLoginForm()">Înapoi la login</button>
            </form>
        `;

        loginForm.insertAdjacentHTML('afterend', registerFormHTML);

        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRegister(e);
            });
        }
        this.showRegisterForm();
    },

    setLoadingState(button, isLoading) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            button.classList.add('loading');
            button.dataset.originalText = button.textContent;
            button.textContent = 'Se conectează...';
        } else {
            button.disabled = false;
            button.classList.remove('loading');
            button.textContent = button.dataset.originalText || 'Conectare';
        }
    },

    showError(message) {
        this.clearMessages();
        const authCard = document.querySelector('.auth-card');
        const errorDiv = document.createElement('div');
        errorDiv.className = 'auth-error';
        errorDiv.style.cssText = 'background: #fed7d7; color: #c53030; padding: 10px; border-radius: 5px; margin: 10px 0; font-size: 14px;';
        errorDiv.textContent = message;

        const authTitle = authCard.querySelector('.auth-title');
        authTitle.insertAdjacentElement('afterend', errorDiv);

        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove();
            }
        }, 5000);
    },

    showSuccess(message) {
        this.clearMessages();
        const authCard = document.querySelector('.auth-card');
        const successDiv = document.createElement('div');
        successDiv.className = 'auth-success';
        successDiv.style.cssText = 'background: #c6f6d5; color: #2f855a; padding: 10px; border-radius: 5px; margin: 10px 0; font-size: 14px;';
        successDiv.textContent = message;

        const authTitle = authCard.querySelector('.auth-title');
        authTitle.insertAdjacentElement('afterend', successDiv);

        setTimeout(() => {
            if (successDiv.parentNode) {
                successDiv.remove();
            }
        }, 5000);
    },

    clearMessages() {
        const messages = document.querySelectorAll('.auth-error, .auth-success');
        messages.forEach(msg => msg.remove());
    }
};