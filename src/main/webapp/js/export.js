window.Export = {
    apiBaseUrl: '/ama/api/export',
    init() {
        console.log('Export module initialized');
    },

    // export o abreviere in format html/markdown
    async exportAbbreviation(abbreviationId, format) {
        if (!abbreviationId || !format) {
            console.error('Parametri invalizi pentru export');
            return;
        }

        const supportedFormats = ['markdown', 'html'];
        if (!supportedFormats.includes(format.toLowerCase())) {
            console.error(`Formatul ${format} nu este suportat pentru export individual`);
            return;
        }

        try {
            await this.downloadFromServer(abbreviationId, format);

        } catch (error) {
            console.error('Eroare la export:', error);
        }
    },

    //export statistici legate de intreaga app in format csv sau pdf
    async exportStatistics(format, filters = {}) {
        if (!format) {
            console.error('Format nespecificat pentru export statistici');
            return;
        }

        const supportedFormats = ['csv', 'pdf'];
        if (!supportedFormats.includes(format.toLowerCase())) {
            console.error(`Formatul ${format} nu este suportat pentru export statistici`);
            return;
        }

        try {
            await this.downloadStatisticsFromServer(format, filters);

        } catch (error) {
            console.error('Eroare la export statistici:', error);
        }
    },

    async downloadStatisticsFromServer(format, filters = {}) {
        const url = `/ama/api/statistics/export/${format}`;

        try {
            console.log('Requesting statistics:', url);

            if (format === 'pdf') {
                // la pdf deschid in tab nou
                window.open(url, '_blank');

            } else {
                // la CSV download
                const response = await window.AMA.authenticatedFetch(url);

                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }

                const content = await response.text();

                // obtinem numele fisierului prin header din back
                const contentDisposition = response.headers.get('content-disposition');
                let filename = `AMA_Statistics_${this.getCurrentDateString()}.${format}`;

                if (contentDisposition) {
                    const filenameMatch = contentDisposition.match(/filename="([^"]+)"/);
                    if (filenameMatch) {
                        filename = filenameMatch[1];
                    }
                }

                this.triggerDownload(content, filename, this.getContentType(format));
            }

        } catch (error) {
            console.error('Error downloading statistics:', error);
            throw new Error(`Eroare la generarea statisticilor: ${error.message}`);
        }
    },

    getCurrentDateString() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    },


    async downloadFromServer(abbreviationId, format) {
        const url = `${this.apiBaseUrl}/abbreviation/${abbreviationId}/${format}`;

        try {
            const response = await window.AMA.authenticatedFetch(url);

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // obt numele fisierului din header din back
            const contentDisposition = response.headers.get('content-disposition');
            let filename = `abbreviation_export.${format}`;

            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="([^"]+)"/);
                if (filenameMatch) {
                    filename = filenameMatch[1];
                }
            }
            const content = await response.text();
            this.triggerDownload(content, filename, this.getContentType(format));

        } catch (error) {
            throw new Error(`Eroare la downloadul de la server: ${error.message}`);
        }
    },

    triggerDownload(content, filename, contentType = 'text/plain') {
        const blob = new Blob([content], { type: contentType + ';charset=utf-8' });
        const url = window.URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.style.display = 'none';

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);

        setTimeout(() => {
            window.URL.revokeObjectURL(url);
        }, 100);
    },

    getContentType(format) {
        const contentTypes = {
            'markdown': 'text/markdown',
            'html': 'text/html',
            'csv': 'text/csv',
            'pdf': 'application/pdf'
        };
        return contentTypes[format] || 'text/plain';
    },
};

// Export pt global acces
if (typeof module !== 'undefined' && module.exports) {
    module.exports = window.Export;
}

// Auto-initialize
document.addEventListener('DOMContentLoaded', () => {
    window.Export.init();
});