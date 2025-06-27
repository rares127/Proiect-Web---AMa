package ama.utils;

import ama.servlets.StatisticsServlet.StatisticsData;
import ama.servlets.StatisticsServlet.LanguageStatistic;
import ama.servlets.StatisticsServlet.DomainStatistic;
import ama.servlets.StatisticsServlet.AbbreviationStatistic;
import ama.servlets.StatisticsServlet.UserStatistic;
import ama.servlets.StatisticsServlet.ActivityStatistic;

import java.text.SimpleDateFormat;
import java.util.Date;


public class StatisticsExporter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy, HH:mm");

    public StatisticsExporter() {
    }

    public String toCSV(StatisticsData statistics) {
        StringBuilder csv = new StringBuilder();

        csv.append("\uFEFF");

        csv.append("# AMA Platform - Raport Statistici\n");
        csv.append("# Generat la: ").append(DATE_FORMAT.format(new Date())).append("\n");
        csv.append("# Total abrevieri: ").append(statistics.totalAbbreviations).append("\n");
        csv.append("# Total utilizatori: ").append(statistics.totalUsers).append("\n");
        csv.append("\n");

        csv.append("## DISTRIBUTIA PE LIMBI\n");
        csv.append("Limba,Cod,Numar Abrevieri,Procent\n");
        if (statistics.languageStats != null) {
            for (LanguageStatistic lang : statistics.languageStats) {
                csv.append(escapeCSV(lang.languageName)).append(",");
                csv.append(escapeCSV(lang.languageCode)).append(",");
                csv.append(lang.count).append(",");
                csv.append(String.format("%.1f%%", lang.percentage)).append("\n");
            }
        }
        csv.append("\n");

        csv.append("## DISTRIBUTIA PE DOMENII\n");
        csv.append("Domeniu,Cod,Numar Abrevieri,Procent\n");
        if (statistics.domainStats != null) {
            for (DomainStatistic domain : statistics.domainStats) {
                csv.append(escapeCSV(domain.domainName)).append(",");
                csv.append(escapeCSV(domain.domainCode)).append(",");
                csv.append(domain.count).append(",");
                csv.append(String.format("%.1f%%", domain.percentage)).append("\n");
            }
        }
        csv.append("\n");

        csv.append("## TOP ABREVIERI DUPA VIZUALIZARI\n");
        csv.append("Pozitie,Abreviere,Limba,Domeniu,Autor,Vizualizari,Aprecieri,Data Crearii\n");
        if (statistics.topByViews != null) {
            for (int i = 0; i < statistics.topByViews.size(); i++) {
                AbbreviationStatistic abbr = statistics.topByViews.get(i);
                csv.append(i + 1).append(",");
                csv.append(escapeCSV(abbr.name)).append(",");
                csv.append(escapeCSV(abbr.language)).append(",");
                csv.append(escapeCSV(abbr.domain)).append(",");
                csv.append(escapeCSV(abbr.author)).append(",");
                csv.append(abbr.views).append(",");
                csv.append(abbr.likes).append(",");
                csv.append(escapeCSV(abbr.createdAt)).append("\n");
            }
        }
        csv.append("\n");

        csv.append("## TOP ABREVIERI DUPA APRECIERI\n");
        csv.append("Pozitie,Abreviere,Limba,Domeniu,Autor,Vizualizari,Aprecieri,Data Crearii\n");
        if (statistics.topByLikes != null) {
            for (int i = 0; i < statistics.topByLikes.size(); i++) {
                AbbreviationStatistic abbr = statistics.topByLikes.get(i);
                csv.append(i + 1).append(",");
                csv.append(escapeCSV(abbr.name)).append(",");
                csv.append(escapeCSV(abbr.language)).append(",");
                csv.append(escapeCSV(abbr.domain)).append(",");
                csv.append(escapeCSV(abbr.author)).append(",");
                csv.append(abbr.views).append(",");
                csv.append(abbr.likes).append(",");
                csv.append(escapeCSV(abbr.createdAt)).append("\n");
            }
        }
        csv.append("\n");

        // Top contributori
        csv.append("## TOP CONTRIBUTORI\n");
        csv.append("Pozitie,Utilizator,Numar Contributii,Total Vizualizari,Total Aprecieri\n");
        if (statistics.topContributors != null) {
            for (int i = 0; i < statistics.topContributors.size(); i++) {
                UserStatistic user = statistics.topContributors.get(i);
                csv.append(i + 1).append(",");
                csv.append(escapeCSV(user.username)).append(",");
                csv.append(user.contributionsCount).append(",");
                csv.append(user.totalViews).append(",");
                csv.append(user.totalLikes).append("\n");
            }
        }
        csv.append("\n");

        csv.append("## ACTIVITATE RECENTA (Ultimele 7 zile)\n");
        csv.append("Data,Abrevieri Noi,Total Vizualizari,Total Aprecieri\n");
        if (statistics.recentActivity != null) {
            for (ActivityStatistic activity : statistics.recentActivity) {
                csv.append(escapeCSV(activity.date)).append(",");
                csv.append(activity.newAbbreviations).append(",");
                csv.append(activity.totalViews).append(",");
                csv.append(activity.totalLikes).append("\n");
            }
        }

        return csv.toString();
    }

    public String toPDF(StatisticsData statistics) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ro\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>AMA Platform - Raport Statistici</title>\n");
        html.append("    <style>\n");
        html.append(getPDFStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        html.append("    <div class=\"header\">\n");
        html.append("        <h1>🔤 AMA Platform</h1>\n");
        html.append("        <h2>Raport Statistici</h2>\n");
        html.append("        <p class=\"generated\">Generat la: ").append(DATE_FORMAT.format(new Date())).append("</p>\n");
        html.append("    </div>\n");

        html.append("    <div class=\"summary\">\n");
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"number\">").append(statistics.totalAbbreviations).append("</div>\n");
        html.append("            <div class=\"label\">Total Abrevieri</div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"number\">").append(statistics.totalUsers).append("</div>\n");
        html.append("            <div class=\"label\">Utilizatori Activi</div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"number\">").append(statistics.languageStats != null ? statistics.languageStats.size() : 0).append("</div>\n");
        html.append("            <div class=\"label\">Limbi Suportate</div>\n");
        html.append("        </div>\n");
        html.append("        <div class=\"summary-card\">\n");
        html.append("            <div class=\"number\">").append(statistics.domainStats != null ? statistics.domainStats.size() : 0).append("</div>\n");
        html.append("            <div class=\"label\">Domenii Acoperite</div>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");

        if (statistics.languageStats != null && !statistics.languageStats.isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h3>📊 Distribuția pe Limbi</h3>\n");
            html.append("        <table class=\"stats-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr><th>Limba</th><th>Cod</th><th>Număr</th><th>Procent</th></tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            for (LanguageStatistic lang : statistics.languageStats) {
                html.append("                <tr>\n");
                html.append("                    <td>").append(escapeHtml(lang.languageName)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(lang.languageCode)).append("</td>\n");
                html.append("                    <td>").append(lang.count).append("</td>\n");
                html.append("                    <td>").append(String.format("%.1f%%", lang.percentage)).append("</td>\n");
                html.append("                </tr>\n");
            }
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
        }

        if (statistics.domainStats != null && !statistics.domainStats.isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h3>🏷️ Distribuția pe Domenii</h3>\n");
            html.append("        <table class=\"stats-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr><th>Domeniu</th><th>Cod</th><th>Număr</th><th>Procent</th></tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            for (DomainStatistic domain : statistics.domainStats) {
                html.append("                <tr>\n");
                html.append("                    <td>").append(escapeHtml(domain.domainName)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(domain.domainCode)).append("</td>\n");
                html.append("                    <td>").append(domain.count).append("</td>\n");
                html.append("                    <td>").append(String.format("%.1f%%", domain.percentage)).append("</td>\n");
                html.append("                </tr>\n");
            }
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
        }

        if (statistics.topByViews != null && !statistics.topByViews.isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h3>👁️ Top Abrevieri după Vizualizări</h3>\n");
            html.append("        <table class=\"stats-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr><th>#</th><th>Abreviere</th><th>Limbă</th><th>Domeniu</th><th>Autor</th><th>Vizualizări</th></tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            for (int i = 0; i < statistics.topByViews.size(); i++) {
                AbbreviationStatistic abbr = statistics.topByViews.get(i);
                html.append("                <tr>\n");
                html.append("                    <td>").append(i + 1).append("</td>\n");
                html.append("                    <td class=\"abbrev-name\">").append(escapeHtml(abbr.name)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(abbr.language)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(abbr.domain)).append("</td>\n");
                html.append("                    <td>").append(escapeHtml(abbr.author)).append("</td>\n");
                html.append("                    <td class=\"number\">").append(abbr.views).append("</td>\n");
                html.append("                </tr>\n");
            }
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
        }

        if (statistics.topContributors != null && !statistics.topContributors.isEmpty()) {
            html.append("    <div class=\"section\">\n");
            html.append("        <h3>🏆 Top Contributori</h3>\n");
            html.append("        <table class=\"stats-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr><th>#</th><th>Utilizator</th><th>Contribuții</th><th>Total Vizualizări</th><th>Total Aprecieri</th></tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");
            for (int i = 0; i < statistics.topContributors.size(); i++) {
                UserStatistic user = statistics.topContributors.get(i);
                html.append("                <tr>\n");
                html.append("                    <td>").append(i + 1).append("</td>\n");
                html.append("                    <td class=\"username\">").append(escapeHtml(user.username)).append("</td>\n");
                html.append("                    <td class=\"number\">").append(user.contributionsCount).append("</td>\n");
                html.append("                    <td class=\"number\">").append(user.totalViews).append("</td>\n");
                html.append("                    <td class=\"number\">").append(user.totalLikes).append("</td>\n");
                html.append("                </tr>\n");
            }
            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
        }

        html.append("    <div class=\"footer\">\n");
        html.append("        <p>Acest raport a fost generat automat de platforma AMA (Abbreviation Management)</p>\n");
        html.append("        <p>Pentru informații suplimentare, contactați administratorul platformei</p>\n");
        html.append("    </div>\n");

        html.append("    <script>\n");
        html.append("        function initPrint() {\n");
        html.append("            try {\n");
        html.append("                if (window.print) {\n");
        html.append("                    window.print();\n");
        html.append("                } else {\n");
        html.append("                    alert('Apăsați Ctrl+P pentru a printa acest raport.');\n");
        html.append("                }\n");
        html.append("            } catch (e) {\n");
        html.append("                console.warn('Print failed:', e);\n");
        html.append("                alert('Pentru a printa raportul, apăsați Ctrl+P sau accesați File > Print din meniu.');\n");
        html.append("            }\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        // Încearcă să printeze după ce pagina s-a încărcat\n");
        html.append("        if (document.readyState === 'loading') {\n");
        html.append("            document.addEventListener('DOMContentLoaded', function() {\n");
        html.append("                setTimeout(initPrint, 1000);\n");
        html.append("            });\n");
        html.append("        } else {\n");
        html.append("            setTimeout(initPrint, 1000);\n");
        html.append("        }\n");
        html.append("        \n");
        html.append("        // Fallback pentru window.onload\n");
        html.append("        window.onload = function() {\n");
        html.append("            setTimeout(initPrint, 1500);\n");
        html.append("        };\n");
        html.append("    </script>\n");

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    private String getPDFStyles() {
        return """
            @media print {
                @page { 
                    margin: 1.5cm; 
                    size: A4;
                }
                body { 
                    font-size: 12pt; 
                    line-height: 1.4;
                }
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
                line-height: 1.6;
                margin: 0;
                padding: 20px;
                color: #333;
                background: white;
            }
            
            .header {
                text-align: center;
                border-bottom: 3px solid #007bff;
                padding-bottom: 20px;
                margin-bottom: 30px;
            }
            
            .header h1 {
                color: #007bff;
                margin: 0;
                font-size: 2.5em;
                font-weight: 300;
            }
            
            .header h2 {
                color: #6c757d;
                margin: 10px 0;
                font-weight: 400;
            }
            
            .generated {
                color: #6c757d;
                font-size: 0.9em;
                margin: 10px 0 0 0;
            }
            
            .summary {
                display: grid;
                grid-template-columns: repeat(4, 1fr);
                gap: 20px;
                margin-bottom: 40px;
            }
            
            .summary-card {
                background: linear-gradient(135deg, #f8f9fa, #e9ecef);
                border: 1px solid #dee2e6;
                border-radius: 8px;
                padding: 20px;
                text-align: center;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            
            .summary-card .number {
                font-size: 2.5em;
                font-weight: bold;
                color: #007bff;
                margin-bottom: 5px;
            }
            
            .summary-card .label {
                color: #6c757d;
                font-size: 0.9em;
                font-weight: 500;
            }
            
            .section {
                margin-bottom: 40px;
                page-break-inside: avoid;
            }
            
            h3 {
                color: #495057;
                border-bottom: 2px solid #e9ecef;
                padding-bottom: 8px;
                margin-bottom: 20px;
                font-size: 1.3em;
            }
            
            .stats-table {
                width: 100%;
                border-collapse: collapse;
                background: white;
                box-shadow: 0 1px 3px rgba(0,0,0,0.1);
                border-radius: 8px;
                overflow: hidden;
            }
            
            .stats-table thead {
                background: #007bff;
                color: white;
            }
            
            .stats-table th,
            .stats-table td {
                padding: 12px 15px;
                text-align: left;
                border-bottom: 1px solid #dee2e6;
            }
            
            .stats-table th {
                font-weight: 600;
                font-size: 0.9em;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            
            .stats-table tbody tr:hover {
                background-color: #f8f9fa;
            }
            
            .stats-table tbody tr:nth-child(even) {
                background-color: #f8f9fa;
            }
            
            .number {
                text-align: right;
                font-weight: 600;
                color: #007bff;
            }
            
            .abbrev-name {
                font-weight: 600;
                color: #495057;
            }
            
            .username {
                font-weight: 500;
                color: #6f42c1;
            }
            
            .footer {
                margin-top: 50px;
                padding-top: 20px;
                border-top: 1px solid #dee2e6;
                text-align: center;
                color: #6c757d;
                font-size: 0.9em;
            }
            
            @media (max-width: 768px) {
                .summary {
                    grid-template-columns: repeat(2, 1fr);
                }
            }
            
            @media (max-width: 480px) {
                .summary {
                    grid-template-columns: 1fr;
                }
                
                .stats-table {
                    font-size: 0.8em;
                }
                
                .stats-table th,
                .stats-table td {
                    padding: 8px 10px;
                }
            }
        """;
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}