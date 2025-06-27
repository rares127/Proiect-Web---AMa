window.ModalManager = {
    modalStack: [],
    baseZIndex: 2000,

    init() {
        console.log('Modal Manager initialized');
        this.bindEvents();
    },

    bindEvents() {
        document.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal-backdrop')) {
                this.closeTopModal();
            }
        });
    },

    openModal(modalId, options = {}) {
        const modal = document.getElementById(modalId);
        if (!modal) {
            console.error('Modal not found:', modalId);
            return false;
        }

        // z-index: atunci cand deschid un modal in timp ce altul este deja deschis, trebuie sa ii dau un z-index mai mare
        const zIndex = this.baseZIndex + (this.modalStack.length * 10);
        modal.style.zIndex = zIndex;

        // il add in stiva
        this.modalStack.push({
            id: modalId,
            element: modal,
            zIndex: zIndex,
            options: options
        });

        // Activez modalul
        modal.classList.add('active');
        return true;
    },

    closeModal(modalId) {
        const modalIndex = this.modalStack.findIndex(modal => modal.id === modalId);
        
        if (modalIndex === -1) {
            return false;
        }

        const modalInfo = this.modalStack[modalIndex];
        const modal = modalInfo.element;

        // dezactivez modalul
        modal.classList.remove('active');
        modal.style.zIndex = '';

        // elimin din stiva
        this.modalStack.splice(modalIndex, 1);
        return true;
    },

    closeTopModal() {
        if (this.modalStack.length > 0) {
            const topModal = this.modalStack[this.modalStack.length - 1];
            this.closeModal(topModal.id);
        }
    },

    closeAllModals() {
        this.modalStack.forEach(modalInfo => {
            modalInfo.element.classList.remove('active');
            modalInfo.element.style.zIndex = '';
        });
        this.modalStack = [];
    },

    isModalOpen(modalId) {
        return this.modalStack.some(modal => modal.id === modalId);
    }
};

// Auto-initialize
document.addEventListener('DOMContentLoaded', () => {
    window.ModalManager.init();
});