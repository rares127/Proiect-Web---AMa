package ama.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ama.models.Abbreviation;
import ama.models.Language;
import ama.models.Domain;
import ama.models.User;
import ama.database.DatabaseManager;

@WebServlet("/api/rss/popular-abbreviations")
public class RSSFeedServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // verific daca este cerere pentru view XML (pentru browser)
        String viewParam = request.getParameter("view");
        boolean isXmlView = "xml".equals(viewParam);

        if (isXmlView) {
            // Pentru view XML în browser - returnează ca text/xml
            response.setContentType("text/xml; charset=UTF-8");
        } else {
            // Pentru RSS readers - returnează ca application/rss+xml
            response.setContentType("application/rss+xml; charset=UTF-8");
        }

        response.setHeader("Cache-Control", "public, max-age=3600"); // Cache 1 oră

        try {
            List<Abbreviation> popularAbbreviations = getPopularAbbreviations(20); // Top 20

            String rssXml = generateRSSFeed(popularAbbreviations, request);

            try (PrintWriter out = response.getWriter()) {
                out.print(rssXml);
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("<?xml version=\"1.0\"?><error>RSS feed unavailable</error>");
        }
    }

    private List<Abbreviation> getPopularAbbreviations(int limit) {
        List<Abbreviation> popular = new ArrayList<>();

        Connection conn = null;
        try {
            conn = DatabaseManager.getInstance().getConnection();
            String sql = """
                SELECT a.*, 
                       (a.views * 0.4 + a.likes * 0.3 + a.favorites * 0.3) as popularity_score,
                       u.username, u.email,
                       l.code as language_code, l.name as language_name,
                       d.code as domain_code, d.name as domain_name
                FROM abbreviations a 
                LEFT JOIN users u ON a.user_id = u.id
                LEFT JOIN languages l ON a.language_id = l.id  
                LEFT JOIN domains d ON a.domain_id = d.id
                ORDER BY popularity_score DESC, a.created_at DESC
                LIMIT ?
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    Abbreviation abbrev = new Abbreviation();

                    abbrev.setId(rs.getInt("id"));
                    abbrev.setName(rs.getString("name"));
                    abbrev.setLanguageId(rs.getInt("language_id"));
                    abbrev.setDomainId(rs.getInt("domain_id"));
                    abbrev.setUserId(rs.getInt("user_id"));
                    abbrev.setDocbook(rs.getString("docbook"));
                    abbrev.setDescription(rs.getString("description"));
                    abbrev.setViews(rs.getInt("views"));
                    abbrev.setLikes(rs.getInt("likes"));
                    abbrev.setFavorites(rs.getInt("favorites"));
                    abbrev.setCreatedAt(rs.getTimestamp("created_at"));

                    if (rs.getString("language_code") != null) {
                        Language language = new Language();
                        language.setId(rs.getInt("language_id"));
                        language.setCode(rs.getString("language_code"));
                        language.setName(rs.getString("language_name"));
                        abbrev.setLanguage(language);
                    }

                    if (rs.getString("domain_code") != null) {
                        Domain domain = new Domain();
                        domain.setId(rs.getInt("domain_id"));
                        domain.setCode(rs.getString("domain_code"));
                        domain.setName(rs.getString("domain_name"));
                        abbrev.setDomain(domain);
                    }

                    if (rs.getString("username") != null) {
                        User user = new User();
                        user.setId(rs.getInt("user_id"));
                        user.setUsername(rs.getString("username"));
                        user.setEmail(rs.getString("email"));
                        abbrev.setUser(user);
                    }

                    List<String> meanings = getMeaningsForAbbreviation(abbrev.getId());
                    abbrev.setMeanings(meanings);

                    popular.add(abbrev);
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    DatabaseManager.getInstance().releaseConnection(conn);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return popular;
    }

    private List<String> getMeaningsForAbbreviation(int abbreviationId) {
        List<String> meanings = new ArrayList<>();
        Connection conn = null;

        try {
            conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT meaning FROM abbreviation_meanings WHERE abbreviation_id = ? ORDER BY id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, abbreviationId);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    meanings.add(rs.getString("meaning"));
                }
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    DatabaseManager.getInstance().releaseConnection(conn);
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return meanings;
    }

    private String generateRSSFeed(List<Abbreviation> abbreviations, HttpServletRequest request) {
        StringBuilder rss = new StringBuilder();

        String baseUrl = getBaseUrl(request);
        String currentDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                .format(new Date());

        // RSS Header
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
        rss.append("  <channel>\n");

        rss.append("    <title>").append(escapeXml("AMa - Abrevieri Populare")).append("</title>\n");
        rss.append("    <description>").append(escapeXml("Clasamentul abrevierilor cele mai accesate din platforma AMa")).append("</description>\n");
        rss.append("    <link>").append(escapeXml(baseUrl)).append("</link>\n");
        rss.append("    <language>ro-RO</language>\n");
        rss.append("    <lastBuildDate>").append(currentDate).append("</lastBuildDate>\n");
        rss.append("    <generator>AMa RSS Generator v1.0</generator>\n");
        rss.append("    <ttl>60</ttl>\n");

        int rank = 1;
        for (Abbreviation abbrev : abbreviations) {
            rss.append("    <item>\n");

            String title = "#" + rank + " - " + abbrev.getName();
            if (abbrev.getMeanings() != null && !abbrev.getMeanings().isEmpty()) {
                title += " (" + abbrev.getMeanings().get(0) + ")";
            }
            rss.append("      <title>").append(escapeXml(title)).append("</title>\n");

            String itemLink = baseUrl + "/?abbrev=" + abbrev.getId();
            rss.append("      <link>").append(escapeXml(itemLink)).append("</link>\n");

            StringBuilder description = new StringBuilder();
            description.append("Abrevierea: ").append(abbrev.getName()).append("\n\n");

            if (abbrev.getMeanings() != null && !abbrev.getMeanings().isEmpty()) {
                description.append("Semnificații:\n");
                for (String meaning : abbrev.getMeanings()) {
                    description.append("• ").append(meaning).append("\n");
                }
                description.append("\n");
            }

            if (abbrev.getDescription() != null && !abbrev.getDescription().trim().isEmpty()) {
                description.append("Descriere: ").append(abbrev.getDescription()).append("\n\n");
            }

            description.append("Statistici: ");
            description.append(abbrev.getViews()).append(" vizualizări, ");
            description.append(abbrev.getLikes()).append(" aprecieri, ");
            description.append(abbrev.getFavorites()).append(" favorite\n\n");

            if (abbrev.getUser() != null) {
                description.append("Autor: ").append(abbrev.getUser().getUsername()).append("\n");
            }

            if (abbrev.getLanguage() != null) {
                description.append("Limbă: ").append(abbrev.getLanguage().getName()).append("\n");
            }

            if (abbrev.getDomain() != null) {
                description.append("Domeniu: ").append(abbrev.getDomain().getName()).append("\n");
            }

            rss.append("      <description>").append(escapeXml(description.toString())).append("</description>\n");

            String guid = "ama-abbrev-" + abbrev.getId() + "-" + abbrev.getViews();
            rss.append("      <guid isPermaLink=\"false\">").append(escapeXml(guid)).append("</guid>\n");

            if (abbrev.getCreatedAt() != null) {
                String pubDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
                        .format(abbrev.getCreatedAt());
                rss.append("      <pubDate>").append(pubDate).append("</pubDate>\n");
            }

            if (abbrev.getUser() != null) {
                rss.append("      <dc:creator>").append(escapeXml(abbrev.getUser().getUsername())).append("</dc:creator>\n");
            }

            if (abbrev.getDomain() != null && abbrev.getDomain().getName() != null) {
                rss.append("      <category>").append(escapeXml(abbrev.getDomain().getName())).append("</category>\n");
            }
            if (abbrev.getLanguage() != null && abbrev.getLanguage().getName() != null) {
                rss.append("      <category>").append(escapeXml(abbrev.getLanguage().getName())).append("</category>\n");
            }

            rss.append("    </item>\n");
            rank++;
        }

        // RSS Footer
        rss.append("  </channel>\n");
        rss.append("</rss>");

        return rss.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", " ")  // Înlocuiește newlines cu spații
                .replaceAll("\\s+", " ")  // Înlocuiește multiple spații cu unul singur
                .trim();
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath);
        return url.toString();
    }
}