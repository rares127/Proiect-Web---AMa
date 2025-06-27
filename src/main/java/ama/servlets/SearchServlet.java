package ama.servlets;

import ama.database.dao.AbbreviationDAO;
import ama.database.dao.UserDAO;
import ama.models.Abbreviation;
import ama.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@WebServlet("/api/search")
public class SearchServlet extends HttpServlet {
    private AbbreviationDAO abbreviationDAO;
    private UserDAO userDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.abbreviationDAO = new AbbreviationDAO();
        this.userDAO = new UserDAO();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setCORSHeaders(response);

        try {
            handleSearch(request, response);
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

    private void handleSearch(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        String searchTerm = request.getParameter("q");  // ?q=HTML
        String languageCode = request.getParameter("language");  // ?language=en
        String domainCode = request.getParameter("domain");    // ?domain=tech
        String sortBy = request.getParameter("sort");          // ?sort=popularity

        int limit = parseIntParameter(request.getParameter("limit"), 20);
        int offset = parseIntParameter(request.getParameter("offset"), 0);

        if (limit > 100) limit = 100; // maxim 100 de rez in pagina de search
        if (limit < 1) limit = 20;
        if (offset < 0) offset = 0;

        searchTerm = normalizeString(searchTerm);
        languageCode = normalizeString(languageCode);
        domainCode = normalizeString(domainCode);

        List<Abbreviation> searchResults = abbreviationDAO.search(searchTerm, languageCode, domainCode);

        searchResults = applySorting(searchResults, sortBy);

        int totalResults = searchResults.size();
        int totalPages = (int) Math.ceil((double) totalResults / limit);
        int currentPage = offset / limit + 1;

        searchResults = applyPagination(searchResults, limit, offset);

        User currentUser = getCurrentUser(request);

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);

        ArrayNode resultsArray = objectMapper.createArrayNode();
        for (Abbreviation abbrev : searchResults) {
            resultsArray.add(abbreviationToJson(abbrev, currentUser));
        }
        responseData.set("results", resultsArray);

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("totalResults", totalResults);
        metadata.put("totalPages", totalPages);
        metadata.put("currentPage", currentPage);
        metadata.put("limit", limit);
        metadata.put("offset", offset);
        metadata.put("hasNext", offset + limit < totalResults);
        metadata.put("hasPrevious", offset > 0);
        responseData.set("metadata", metadata);

        ObjectNode queryInfo = objectMapper.createObjectNode();
        queryInfo.put("searchTerm", searchTerm != null ? searchTerm : "");
        queryInfo.put("language", languageCode != null ? languageCode : "");
        queryInfo.put("domain", domainCode != null ? domainCode : "");
        queryInfo.put("sort", sortBy != null ? sortBy : "alphabetical");
        responseData.set("query", queryInfo);

        if (totalResults == 0 && (searchTerm != null || languageCode != null || domainCode != null)) {
            ObjectNode suggestions = objectMapper.createObjectNode();
            suggestions.put("message", "Nu au fost găsite rezultate pentru această căutare");

            ArrayNode tips = objectMapper.createArrayNode();
            tips.add("Încearcă să folosești termeni mai generali");
            tips.add("Verifică ortografia");
            tips.add("Încearcă să cauți doar după abreviere");
            tips.add("Elimină filtrele de limbă sau domeniu");
            suggestions.set("tips", tips);

            responseData.set("suggestions", suggestions);
        }

        sendSuccessResponse(response, responseData);
    }

    private List<Abbreviation> applySorting(List<Abbreviation> abbreviations, String sortBy) {
        if (sortBy == null) {
            sortBy = "alphabetical";
        }

        switch (sortBy.toLowerCase()) {
            case "popularity":
                abbreviations.sort((a, b) -> {
                    int viewsCompare = Integer.compare(b.getViews(), a.getViews());
                    if (viewsCompare != 0) return viewsCompare;
                    return Integer.compare(b.getLikes(), a.getLikes());
                });
                break;

            case "date":
            case "newest":
                abbreviations.sort((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                        return 0;
                    }
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;

            case "oldest":
                abbreviations.sort((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) {
                        return 0;
                    }
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                });
                break;

            case "likes":
                abbreviations.sort((a, b) -> Integer.compare(b.getLikes(), a.getLikes()));
                break;

            case "alphabetical":
            default:
                abbreviations.sort((a, b) -> {
                    String nameA = a.getName() != null ? a.getName() : "";
                    String nameB = b.getName() != null ? b.getName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                });
                break;
        }

        return abbreviations;
    }

    private List<Abbreviation> applyPagination(List<Abbreviation> abbreviations, int limit, int offset) {
        int start = Math.min(offset, abbreviations.size());
        int end = Math.min(start + limit, abbreviations.size());
        return abbreviations.subList(start, end);
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

    private ObjectNode abbreviationToJson(Abbreviation abbreviation, User currentUser) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("id", abbreviation.getId());
        json.put("name", abbreviation.getName());
        json.put("description", abbreviation.getDescription());
        json.put("views", abbreviation.getViews());
        json.put("likes", abbreviation.getLikes());
        json.put("favorites", abbreviation.getFavorites());
        json.put("createdAt", abbreviation.getCreatedAt() != null ? abbreviation.getCreatedAt().toString() : null);

        ArrayNode meaningsArray = objectMapper.createArrayNode();
        if (abbreviation.getMeanings() != null) {
            for (String meaning : abbreviation.getMeanings()) {
                meaningsArray.add(meaning);
            }
        }
        json.set("meanings", meaningsArray);

        if (abbreviation.getLanguage() != null) {
            ObjectNode languageJson = objectMapper.createObjectNode();
            languageJson.put("code", abbreviation.getLanguage().getCode());
            languageJson.put("name", abbreviation.getLanguage().getName());
            json.set("language", languageJson);
        }

        if (abbreviation.getDomain() != null) {
            ObjectNode domainJson = objectMapper.createObjectNode();
            domainJson.put("code", abbreviation.getDomain().getCode());
            domainJson.put("name", abbreviation.getDomain().getName());
            json.set("domain", domainJson);
        }

        // Author info
        if (abbreviation.getUser() != null) {
            ObjectNode authorJson = objectMapper.createObjectNode();
            authorJson.put("username", abbreviation.getUser().getUsername());
            json.set("author", authorJson);
        }

        if (currentUser != null) {
            try {
                json.put("isLiked", abbreviationDAO.userHasLiked(abbreviation.getId(), currentUser.getId()));
                json.put("isFavorited", abbreviationDAO.userHasFavorited(abbreviation.getId(), currentUser.getId()));

                boolean canEdit = currentUser.isAdmin() || currentUser.getId() == abbreviation.getUserId();
                boolean canDelete = currentUser.isAdmin() || currentUser.getId() == abbreviation.getUserId();
                json.put("canEdit", canEdit);
                json.put("canDelete", canDelete);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            json.put("isLiked", false);
            json.put("isFavorited", false);
            json.put("canEdit", false);
            json.put("canDelete", false);
        }

        return json;
    }

    private String normalizeString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    private int parseIntParameter(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

    private void sendSuccessResponse(HttpServletResponse response, ObjectNode data) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(data));
        out.flush();
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        ObjectNode errorData = objectMapper.createObjectNode();
        errorData.put("success", false);
        errorData.put("message", message);

        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(errorData));
        out.flush();
    }
}