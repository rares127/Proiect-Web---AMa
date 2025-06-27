package ama.servlets;

import ama.database.dao.AbbreviationDAO;
import ama.database.dao.UserDAO;
import ama.models.User;
import ama.utils.StatisticsExporter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/api/statistics/*")
public class StatisticsServlet extends HttpServlet {
    private AbbreviationDAO abbreviationDAO;
    private UserDAO userDAO;
    private StatisticsExporter statisticsExporter;

    @Override
    public void init() throws ServletException {
        super.init();
        this.abbreviationDAO = new AbbreviationDAO();
        this.userDAO = new UserDAO();
        this.statisticsExporter = new StatisticsExporter();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        setCORSHeaders(response);

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/export/\\w+")) {
                // GET /api/statistics/export/{format}
                String format = pathInfo.substring(8).toLowerCase(); // Remove "/export/"
                handleExportStatistics(request, response, format);
            } else {
                sendErrorResponse(response, 404, "Endpoint de statistici nu există");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare internă a serverului");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private void handleExportStatistics(HttpServletRequest request, HttpServletResponse response, String format)
            throws IOException, SQLException {

        if (!format.equals("csv") && !format.equals("pdf")) {
            sendErrorResponse(response, 400, "Format nesuportat: " + format + ". Formate suportate: csv, pdf");
            return;
        }

        User currentUser = getCurrentUser(request);

        try {
            StatisticsData statistics = collectStatistics();

            String exportedContent;
            String contentType;
            String fileExtension;

            switch (format) {
                case "csv":
                    exportedContent = statisticsExporter.toCSV(statistics);
                    contentType = "text/csv; charset=UTF-8";
                    fileExtension = "csv";
                    break;

                case "pdf":
                    exportedContent = statisticsExporter.toPDF(statistics);
                    contentType = "text/html; charset=UTF-8";
                    fileExtension = "html";
                    break;

                default:
                    sendErrorResponse(response, 400, "Format nesuportat");
                    return;
            }

            response.setContentType(contentType);
            response.setCharacterEncoding("UTF-8");

            if (format.equals("csv")) {
                String filename = "AMA_Statistics_" + getCurrentDateString() + "." + fileExtension;
                response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            } else {
                response.setHeader("Content-Disposition", "inline");
            }

            response.setHeader("Cache-Control", "no-cache");

            response.getWriter().write(exportedContent);
            response.getWriter().flush();

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la generarea statisticilor");
        }
    }

    private StatisticsData collectStatistics() throws SQLException {
        StatisticsData stats = new StatisticsData();

        try {
            try {
                stats.totalAbbreviations = abbreviationDAO.getTotalCount();
            } catch (Exception e) {
            }

            try {
                stats.languageStats = abbreviationDAO.getStatisticsByLanguage();
            } catch (Exception e) {
            }

            try {
                stats.domainStats = abbreviationDAO.getStatisticsByDomain();
            } catch (Exception e) {
            }

            try {
                stats.topByViews = abbreviationDAO.getTopByViews(10);
            } catch (Exception e) {
            }

            try {
                stats.topByLikes = abbreviationDAO.getTopByLikes(10);
            } catch (Exception e) {
            }

            try {
                stats.totalUsers = userDAO.getTotalActiveUsers();
                stats.topContributors = userDAO.getTopContributors(5);
            } catch (Exception e) {
            }

            try {
                stats.recentActivity = abbreviationDAO.getRecentActivity(7);
            } catch (Exception e) {
            }

        } catch (Exception e) {
            System.err.println("Error collecting statistics: " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Failed to collect statistics", e);
        }

        return stats;
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            return null;
        }

        try {
            int userId = (Integer) session.getAttribute("user_id");
            Optional<User> userOpt = userDAO.findById(userId);
            return userOpt.orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getCurrentDateString() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return now.toString(); // Format: YYYY-MM-DD
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json; charset=UTF-8");

        PrintWriter out = response.getWriter();
        out.print("{\"success\": false, \"message\": \"" + message.replace("\"", "\\\"") + "\"}");
        out.flush();
    }

    public static class StatisticsData {
        public int totalAbbreviations;
        public int totalUsers;
        public java.util.List<LanguageStatistic> languageStats;
        public java.util.List<DomainStatistic> domainStats;
        public java.util.List<AbbreviationStatistic> topByViews;
        public java.util.List<AbbreviationStatistic> topByLikes;
        public java.util.List<UserStatistic> topContributors;
        public java.util.List<ActivityStatistic> recentActivity;
    }

    public static class LanguageStatistic {
        public String languageName;
        public String languageCode;
        public int count;
        public double percentage;
    }

    public static class DomainStatistic {
        public String domainName;
        public String domainCode;
        public int count;
        public double percentage;
    }

    public static class AbbreviationStatistic {
        public String name;
        public String language;
        public String domain;
        public String author;
        public int views;
        public int likes;
        public String createdAt;
    }

    public static class UserStatistic {
        public String username;
        public int contributionsCount;
        public int totalViews;
        public int totalLikes;
    }

    public static class ActivityStatistic {
        public String date;
        public int newAbbreviations;
        public int totalViews;
        public int totalLikes;
    }
}