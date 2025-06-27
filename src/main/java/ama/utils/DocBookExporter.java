package ama.utils;

import ama.models.Abbreviation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

///  convertesc dockbook in format html si markdown
public class DocBookExporter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy");

    public DocBookExporter() {
    }

    public String toMarkdown(Abbreviation abbreviation) throws Exception {
        StringBuilder markdown = new StringBuilder();

        markdown.append("# ").append(abbreviation.getName()).append("\n\n");

        // Metadate
        markdown.append("**Limbă:** ").append(getLanguageName(abbreviation)).append("  \n");
        markdown.append("**Domeniu:** ").append(getDomainName(abbreviation)).append("  \n");
        markdown.append("**Autor:** ").append(getAuthorName(abbreviation)).append("  \n");
        markdown.append("**Data creării:** ").append(formatDate(abbreviation.getCreatedAt())).append("  \n");
        markdown.append("**Vizualizări:** ").append(abbreviation.getViews()).append("  \n");
        markdown.append("**Aprecieri:** ").append(abbreviation.getLikes()).append("  \n");
        markdown.append("**Favorite:** ").append(abbreviation.getFavorites()).append("  \n\n");

        // Descriere
        if (abbreviation.getDescription() != null && !abbreviation.getDescription().trim().isEmpty()) {
            markdown.append("## Descriere\n\n");
            markdown.append(abbreviation.getDescription()).append("\n\n");
        }

        // Semnificații din DocBook
        List<String> meanings = extractMeaningsFromDocBook(abbreviation.getDocbook());
        if (!meanings.isEmpty()) {
            markdown.append("## Semnificații\n\n");
            for (int i = 0; i < meanings.size(); i++) {
                markdown.append(i + 1).append(". ").append(meanings.get(i)).append("\n");
            }
            markdown.append("\n");
        }

        // Conținut DocBook original
        if (abbreviation.getDocbook() != null && !abbreviation.getDocbook().trim().isEmpty()) {
            markdown.append("---\n\n");
            markdown.append("*Export generat din platforma AMA (Abbreviation Management) la data de ")
                    .append(DATE_FORMAT.format(new Date())).append("*\n");
        }

        return markdown.toString();
    }

    public String toHTML(Abbreviation abbreviation) throws Exception {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"ro\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(escapeHtml(abbreviation.getName())).append(" - AMA Export</title>\n");
        html.append("    <style>\n");
        html.append(getHTMLStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");

        html.append("    <div class=\"container\">\n");

        // Header
        html.append("        <header class=\"header\">\n");
        html.append("            <h1 class=\"title\">").append(escapeHtml(abbreviation.getName())).append("</h1>\n");
        html.append("            <p class=\"subtitle\">Export din platforma AMA</p>\n");
        html.append("        </header>\n");

        // Metadatele
        html.append("        <section class=\"metadata\">\n");
        html.append("            <h2>Informații generale</h2>\n");
        html.append("            <div class=\"metadata-grid\">\n");

        html.append("                <div class=\"metadata-card\">\n");
        html.append("                    <div class=\"metadata-label\">Limbă</div>\n");
        html.append("                    <div class=\"metadata-value\">").append(escapeHtml(getLanguageName(abbreviation))).append("</div>\n");
        html.append("                </div>\n");

        html.append("                <div class=\"metadata-card\">\n");
        html.append("                    <div class=\"metadata-label\">Domeniu</div>\n");
        html.append("                    <div class=\"metadata-value\">").append(escapeHtml(getDomainName(abbreviation))).append("</div>\n");
        html.append("                </div>\n");

        html.append("                <div class=\"metadata-card\">\n");
        html.append("                    <div class=\"metadata-label\">Autor</div>\n");
        html.append("                    <div class=\"metadata-value\">").append(escapeHtml(getAuthorName(abbreviation))).append("</div>\n");
        html.append("                </div>\n");

        html.append("                <div class=\"metadata-card\">\n");
        html.append("                    <div class=\"metadata-label\">Data creării</div>\n");
        html.append("                    <div class=\"metadata-value\">").append(escapeHtml(formatDate(abbreviation.getCreatedAt()))).append("</div>\n");
        html.append("                </div>\n");

        html.append("            </div>\n");
        html.append("        </section>\n");

        // Statistici
        html.append("        <section class=\"statistics\">\n");
        html.append("            <h2>Statistici</h2>\n");
        html.append("            <div class=\"stats-grid\">\n");
        html.append("                <div class=\"stat-card\">\n");
        html.append("                    <div class=\"stat-number\">").append(abbreviation.getViews()).append("</div>\n");
        html.append("                    <div class=\"stat-label\">Vizualizări</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"stat-card\">\n");
        html.append("                    <div class=\"stat-number\">").append(abbreviation.getLikes()).append("</div>\n");
        html.append("                    <div class=\"stat-label\">Aprecieri</div>\n");
        html.append("                </div>\n");
        html.append("                <div class=\"stat-card\">\n");
        html.append("                    <div class=\"stat-number\">").append(abbreviation.getFavorites()).append("</div>\n");
        html.append("                    <div class=\"stat-label\">Favorite</div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
        html.append("        </section>\n");

        // Descriere
        if (abbreviation.getDescription() != null && !abbreviation.getDescription().trim().isEmpty()) {
            html.append("        <section class=\"description\">\n");
            html.append("            <h2>Descriere</h2>\n");
            html.append("            <div class=\"content\">\n");
            html.append("                <p>").append(escapeHtml(abbreviation.getDescription())).append("</p>\n");
            html.append("            </div>\n");
            html.append("        </section>\n");
        }

        // Semnif din docbook
        List<String> meanings = extractMeaningsFromDocBook(abbreviation.getDocbook());
        if (!meanings.isEmpty()) {
            html.append("        <section class=\"meanings\">\n");
            html.append("            <h2>Semnificații</h2>\n");
            html.append("            <div class=\"content\">\n");
            html.append("                <ol class=\"meanings-list\">\n");
            for (String meaning : meanings) {
                html.append("                    <li>").append(escapeHtml(meaning)).append("</li>\n");
            }
            html.append("                </ol>\n");
            html.append("            </div>\n");
            html.append("        </section>\n");
        }

        // Footer
        html.append("        <footer class=\"footer\">\n");
        html.append("            <p>Export generat din platforma AMA (Abbreviation Management) la data de ")
                .append(DATE_FORMAT.format(new Date())).append("</p>\n");
        html.append("        </footer>\n");

        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }


    private List<String> extractMeaningsFromDocBook(String docbookContent) {
        List<String> meanings = new ArrayList<>();

        if (docbookContent == null || docbookContent.trim().isEmpty()) {
            return meanings;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parsez XML-ul DocBook
            Document doc = builder.parse(new ByteArrayInputStream(docbookContent.getBytes(StandardCharsets.UTF_8)));

            /// caut semnficilatiile
            NodeList listItems = doc.getElementsByTagName("listitem");
            for (int i = 0; i < listItems.getLength(); i++) {
                Element listItem = (Element) listItems.item(i);
                NodeList paras = listItem.getElementsByTagName("para");
                for (int j = 0; j < paras.getLength(); j++) {
                    String meaning = paras.item(j).getTextContent().trim();
                    if (!meaning.isEmpty()) {
                        meanings.add(meaning);
                    }
                }
            }

            if (meanings.isEmpty()) {
                NodeList allParas = doc.getElementsByTagName("para");
                for (int i = 0; i < allParas.getLength(); i++) {
                    String content = allParas.item(i).getTextContent().trim();
                    // Evită să iei descrierea generală
                    if (!content.isEmpty() && !isGeneralDescription(content)) {
                        meanings.add(content);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Eroare la parsarea DocBook: " + e.getMessage());
            e.printStackTrace();
        }

        return meanings;
    }

    private boolean isGeneralDescription(String text) {
        // Heuristici simple pentru a evita descrierile generale
        return text.length() > 100 || // Textele foarte lungi sunt probabil descrieri
                text.toLowerCase().contains("descriere") ||
                text.toLowerCase().contains("este o") ||
                text.toLowerCase().contains("reprezintă");
    }

    private String getHTMLStyles() {
        return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                line-height: 1.6;
                margin: 0;
                padding: 20px;
                background-color: #f5f7fa;
                color: #333;
            }
            
            .container {
                max-width: 800px;
                margin: 0 auto;
                background: white;
                border-radius: 12px;
                box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                overflow: hidden;
            }
            
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 40px 30px;
                text-align: center;
            }
            
            .title {
                margin: 0;
                font-size: 2.5em;
                font-weight: 300;
                letter-spacing: -1px;
            }
            
            .subtitle {
                margin: 10px 0 0 0;
                opacity: 0.9;
                font-size: 1.1em;
            }
            
            section {
                padding: 30px;
                border-bottom: 1px solid #eee;
            }
            
            section:last-of-type {
                border-bottom: none;
            }
            
            h2 {
                color: #2c3e50;
                margin: 0 0 20px 0;
                font-size: 1.5em;
                font-weight: 600;
            }
            
            .metadata-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 15px;
            }
            
            .metadata-card {
                background: #f8f9fa;
                padding: 15px;
                border-radius: 8px;
                border-left: 4px solid #667eea;
            }
            
            .metadata-label {
                font-size: 0.9em;
                color: #666;
                margin-bottom: 5px;
                font-weight: 500;
            }
            
            .metadata-value {
                font-size: 1.1em;
                color: #2c3e50;
                font-weight: 600;
            }
            
            .stats-grid {
                display: grid;
                grid-template-columns: repeat(3, 1fr);
                gap: 20px;
            }
            
            .stat-card {
                text-align: center;
                padding: 20px;
                background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                color: white;
                border-radius: 12px;
            }
            
            .stat-number {
                font-size: 2.5em;
                font-weight: 700;
                margin-bottom: 5px;
            }
            
            .stat-label {
                font-size: 0.9em;
                opacity: 0.9;
            }
            
            .content {
                font-size: 1.1em;
                line-height: 1.7;
            }
            
            .meanings-list {
                counter-reset: meaning-counter;
                list-style: none;
                padding: 0;
            }
            
            .meanings-list li {
                counter-increment: meaning-counter;
                margin-bottom: 15px;
                padding: 15px;
                background: #f8f9fa;
                border-radius: 8px;
                border-left: 4px solid #28a745;
                position: relative;
            }
            
            .meanings-list li::before {
                content: counter(meaning-counter);
                position: absolute;
                left: -15px;
                top: 15px;
                background: #28a745;
                color: white;
                width: 25px;
                height: 25px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-weight: bold;
                font-size: 0.9em;
            }
            
            .footer {
                background: #f8f9fa;
                padding: 20px 30px;
                text-align: center;
                color: #666;
                font-size: 0.9em;
                border-top: 1px solid #eee;
            }
            
            @media (max-width: 600px) {
                .stats-grid {
                    grid-template-columns: 1fr;
                }
                
                .metadata-grid {
                    grid-template-columns: 1fr;
                }
                
                .container {
                    margin: 10px;
                }
                
                body {
                    padding: 10px;
                }
            }
        """;
    }


    private String getLanguageName(Abbreviation abbreviation) {
        if (abbreviation.getLanguage() != null) {
            return abbreviation.getLanguage().getName();
        }
        return "Necunoscut";
    }

    private String getDomainName(Abbreviation abbreviation) {
        if (abbreviation.getDomain() != null) {
            return abbreviation.getDomain().getName();
        }
        return "General";
    }

    private String getAuthorName(Abbreviation abbreviation) {
        if (abbreviation.getUser() != null) {
            return abbreviation.getUser().getUsername();
        }
        return "Necunoscut";
    }

    private String formatDate(java.sql.Timestamp timestamp) {
        if (timestamp != null) {
            return DATE_FORMAT.format(new Date(timestamp.getTime()));
        }
        return "Necunoscut";
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