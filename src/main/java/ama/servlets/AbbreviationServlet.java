package ama.servlets;

import ama.database.dao.AbbreviationDAO;
import ama.database.dao.UserDAO;
import ama.models.Abbreviation;
import ama.models.User;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@WebServlet("/api/abbreviations/*")
public class AbbreviationServlet extends HttpServlet {
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

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        setCORSHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/abbreviations - Lista abrevierilor
                handleGetAbbreviations(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/abbreviations/{id} - Detalii abreviere
                int id = Integer.parseInt(pathInfo.substring(1));
                handleGetAbbreviation(request, response, id);
            } else if (pathInfo.equals("/popular")) {
                // GET /api/abbreviations/popular - Abrevieri populare
                handleGetPopularAbbreviations(request, response);
            } else if (pathInfo.matches("/\\d+/check-permissions")) {
                // GET /api/abbreviations/{id}/check-permissions
                int id = Integer.parseInt(pathInfo.substring(1, pathInfo.lastIndexOf('/')));
                handleCheckPermissions(request, response, id);
            } else {
                sendErrorResponse(response, 404, "Endpoint nu există");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, 400, "ID invalid");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare internă a serverului");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        setCORSHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/abbreviations - creez abrev noua
                handleCreateAbbreviation(request, response);
            } else if (pathInfo.matches("/\\d+/like")) {
                // POST /api/abbreviations/{id}/like - Toggle like
                int id = Integer.parseInt(pathInfo.substring(1, pathInfo.lastIndexOf('/')));
                handleToggleLike(request, response, id);
            } else if (pathInfo.matches("/\\d+/favorite")) {
                // POST /api/abbreviations/{id}/favorite - Toggle favorite
                int id = Integer.parseInt(pathInfo.substring(1, pathInfo.lastIndexOf('/')));
                handleToggleFavorite(request, response, id);
            } else if (pathInfo.matches("/\\d+/view")) {
                // POST /api/abbreviations/{id}/view - cresc vieews
                int id = Integer.parseInt(pathInfo.substring(1, pathInfo.lastIndexOf('/')));
                handleIncrementViews(request, response, id);
            } else {
                sendErrorResponse(response, 404, "Endpoint nu există");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, 400, "ID invalid");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare internă a serverului");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        setCORSHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // PUT /api/abbreviations/{id} - actualizez abrevierea
                int id = Integer.parseInt(pathInfo.substring(1));
                handleUpdateAbbreviation(request, response, id);
            } else {
                sendErrorResponse(response, 404, "Endpoint nu există");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, 400, "ID invalid");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare internă a serverului");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setCORSHeaders(response);
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/\\d+")) {
                // DELETE /api/abbreviations/{id}
                int id = Integer.parseInt(pathInfo.substring(1));
                handleDeleteAbbreviation(request, response, id);
            } else {
                sendErrorResponse(response, 404, "Endpoint nu există");
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(response, 400, "ID invalid");
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


     ///GET /api/abbreviations - Lista abrevierilor cu filtrare
    private void handleGetAbbreviations(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        String searchTerm = request.getParameter("search");
        String languageCode = request.getParameter("language");
        String domainCode = request.getParameter("domain");
        String sortBy = request.getParameter("sort");

        String filter = request.getParameter("filter"); // "mine", "favorites", "all"

        // paginarea
        int limit = parseIntParameter(request.getParameter("limit"), 50);
        int offset = parseIntParameter(request.getParameter("offset"), 0);

        User currentUser = getCurrentUser(request);

        List<Abbreviation> abbreviations;

        if (filter != null && currentUser != null) {
            abbreviations = getFilteredAbbreviations(filter, currentUser, limit, offset);
        } else {
            abbreviations = abbreviationDAO.search(
                    searchTerm != null ? searchTerm : "",
                    languageCode != null ? languageCode : "",
                    domainCode != null ? domainCode : ""
            );

            abbreviations = applyPagination(abbreviations, limit, offset);
        }

        // raspuns JSON
        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);

        ArrayNode abbreviationsArray = objectMapper.createArrayNode();
        for (Abbreviation abbrev : abbreviations) {
            abbreviationsArray.add(abbreviationToJson(abbrev, currentUser));
        }
        responseData.set("abbreviations", abbreviationsArray);

        // Metadate pentru paginare
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("limit", limit);
        metadata.put("offset", offset);
        metadata.put("count", abbreviations.size());
        responseData.set("metadata", metadata);

        sendSuccessResponse(response, responseData);
    }

     ///GET /api/abbreviations/{id} - Detalii abreviere
    private void handleGetAbbreviation(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        Optional<Abbreviation> abbreviationOpt = abbreviationDAO.findById(id);

        if (abbreviationOpt.isPresent()) {
            Abbreviation abbreviation = abbreviationOpt.get();
            User currentUser = getCurrentUser(request);

            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("success", true);
            responseData.set("abbreviation", abbreviationToJson(abbreviation, currentUser));

            sendSuccessResponse(response, responseData);
        } else {
            sendErrorResponse(response, 404, "Abrevierea nu a fost găsită");
        }
    }

    // GET /api/abbreviations/popular - Abrevieri populare
    private void handleGetPopularAbbreviations(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        int limit = parseIntParameter(request.getParameter("limit"), 10);

        List<Abbreviation> popularAbbreviations = abbreviationDAO.getMostPopular(limit);
        User currentUser = getCurrentUser(request);

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);

        ArrayNode abbreviationsArray = objectMapper.createArrayNode();
        for (Abbreviation abbrev : popularAbbreviations) {
            abbreviationsArray.add(abbreviationToJson(abbrev, currentUser));
        }
        responseData.set("abbreviations", abbreviationsArray);

        sendSuccessResponse(response, responseData);
    }


    // POST /api/abbreviations - Creează abreviere nouă
    private void handleCreateAbbreviation(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        // ciitim datele json din request
        JsonNode requestData = objectMapper.readTree(request.getReader());

        String name = getRequiredString(requestData, "name");
        String description = requestData.has("description") ? requestData.get("description").asText() : null;
        String languageCode = getRequiredString(requestData, "language");
        String domainCode = getRequiredString(requestData, "domain");

        List<String> meanings;
        if (requestData.get("meanings").isArray()) {
            meanings = objectMapper.convertValue(requestData.get("meanings"), List.class);
        } else {
            meanings = Arrays.asList(requestData.get("meanings").asText().split(","));
        }
        meanings.removeIf(String::isEmpty);

        if (meanings.isEmpty()) {
            sendErrorResponse(response, 400, "Cel puțin o semnificație este obligatorie");
            return;
        }

        int languageId = getLanguageId(languageCode);
        int domainId = getDomainId(domainCode);

        if (languageId == -1 || domainId == -1) {
            sendErrorResponse(response, 400, "Limbă sau domeniu invalid");
            return;
        }

        Abbreviation abbreviation = new Abbreviation();
        abbreviation.setName(name.trim());
        abbreviation.setDescription(description);
        abbreviation.setLanguageId(languageId);
        abbreviation.setDomainId(domainId);
        abbreviation.setUserId(currentUser.getId());

        abbreviation.setDocbook(generateDocBook(name, meanings, description));

        try {
            Abbreviation createdAbbreviation = abbreviationDAO.create(abbreviation);

            abbreviationDAO.addMeanings(createdAbbreviation.getId(), meanings);

            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("success", true);
            responseData.put("message", "Abrevierea a fost creată cu succes");
            responseData.set("abbreviation", abbreviationToJson(createdAbbreviation, currentUser));

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate key") ||
                    e.getMessage().contains("UNIQUE") ||
                    e.getMessage().contains("există deja")) {
                sendErrorResponse(response, 409, "Abrevierea există deja în această combinație de limbă și domeniu");
            } else {
                throw e;
            }
        }
    }

    ///PUT /api/abbreviations/{id} - update abreviere
    private void handleUpdateAbbreviation(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        Optional<Abbreviation> abbreviationOpt = abbreviationDAO.findById(id);
        if (!abbreviationOpt.isPresent()) {
            sendErrorResponse(response, 404, "Abrevierea nu a fost găsită");
            return;
        }

        Abbreviation existingAbbreviation = abbreviationOpt.get();

        if (!canUserEditAbbreviation(currentUser, existingAbbreviation)) {
            sendErrorResponse(response, 403, "Nu aveți permisiunea să editați această abreviere");
            return;
        }

        JsonNode requestData = objectMapper.readTree(request.getReader());

        String name = getRequiredString(requestData, "name");
        String description = requestData.has("description") ? requestData.get("description").asText() : null;
        String languageCode = getRequiredString(requestData, "language");
        String domainCode = getRequiredString(requestData, "domain");

        List<String> meanings;
        if (requestData.get("meanings").isArray()) {
            meanings = objectMapper.convertValue(requestData.get("meanings"), List.class);
        } else {
            meanings = Arrays.asList(requestData.get("meanings").asText().split(","));
        }
        meanings.removeIf(String::isEmpty);

        if (meanings.isEmpty()) {
            sendErrorResponse(response, 400, "Cel puțin o semnificație este obligatorie");
            return;
        }

        int languageId = getLanguageId(languageCode);
        int domainId = getDomainId(domainCode);

        if (languageId == -1 || domainId == -1) {
            sendErrorResponse(response, 400, "Limbă sau domeniu invalid");
            return;
        }

        try {
            existingAbbreviation.setName(name.trim());
            existingAbbreviation.setDescription(description);
            existingAbbreviation.setLanguageId(languageId);
            existingAbbreviation.setDomainId(domainId);
            existingAbbreviation.setDocbook(generateDocBook(name, meanings, description));

            abbreviationDAO.update(existingAbbreviation);

            abbreviationDAO.clearMeanings(id); ///sterg semnificatia veche
            abbreviationDAO.addMeanings(id, meanings);

            Optional<Abbreviation> updatedOpt = abbreviationDAO.findById(id);
            Abbreviation updatedAbbreviation = updatedOpt.get();

            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("success", true);
            responseData.put("message", "Abrevierea a fost actualizată cu succes");
            responseData.set("abbreviation", abbreviationToJson(updatedAbbreviation, currentUser));

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la actualizarea abrevierii");
        }
    }


     //DELETE /api/abbreviations/{id} - delete abreviere
    private void handleDeleteAbbreviation(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        Optional<Abbreviation> abbreviationOpt = abbreviationDAO.findById(id);
        if (!abbreviationOpt.isPresent()) {
            sendErrorResponse(response, 404, "Abrevierea nu a fost găsită");
            return;
        }

        Abbreviation abbreviation = abbreviationOpt.get();

        if (!canUserDeleteAbbreviation(currentUser, abbreviation)) {
            sendErrorResponse(response, 403, "Nu aveți permisiunea să ștergeți această abreviere");
            return;
        }

        try {
            boolean deleted = abbreviationDAO.delete(id);

            if (deleted) {
                ObjectNode responseData = objectMapper.createObjectNode();
                responseData.put("success", true);
                responseData.put("message", "Abrevierea a fost ștearsă cu succes");

                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, 500, "Nu s-a putut șterge abrevierea");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la ștergerea abrevierii");
        }
    }

    ///POST /api/abbreviations/{id}/like - Toggle like
    private void handleToggleLike(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        boolean liked = abbreviationDAO.toggleLike(id, currentUser.getId());

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);
        responseData.put("liked", liked);
        responseData.put("message", liked ? "Abreviere apreciată" : "Apreciere eliminată");

        sendSuccessResponse(response, responseData);
    }

     ///POST /api/abbreviations/{id}/favorite - Toggle favorite
    private void handleToggleFavorite(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        boolean favorited = abbreviationDAO.toggleFavorite(id, currentUser.getId());

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);
        responseData.put("favorited", favorited);
        responseData.put("message", favorited ? "Adăugat la favorite" : "Eliminat din favorite");

        sendSuccessResponse(response, responseData);
    }

     //POST /api/abbreviations/{id}/view - incremnetez vies
    private void handleIncrementViews(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        abbreviationDAO.incrementViews(id);

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);
        responseData.put("message", "Vizualizare înregistrată");

        sendSuccessResponse(response, responseData);
    }


     //GET /api/abbreviations/{id}/check-permissions - verific permisiunile userului
    private void handleCheckPermissions(HttpServletRequest request, HttpServletResponse response, int id)
            throws IOException, SQLException {

        User currentUser = getCurrentUser(request);

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);

        ObjectNode permissions = objectMapper.createObjectNode();
        permissions.put("canEdit", false);
        permissions.put("canDelete", false);
        permissions.put("canLike", currentUser != null);
        permissions.put("canFavorite", currentUser != null);

        if (currentUser != null) {
            Optional<Abbreviation> abbreviationOpt = abbreviationDAO.findById(id);
            if (abbreviationOpt.isPresent()) {
                Abbreviation abbreviation = abbreviationOpt.get();
                permissions.put("canEdit", canUserEditAbbreviation(currentUser, abbreviation));
                permissions.put("canDelete", canUserDeleteAbbreviation(currentUser, abbreviation));
            }
        }

        responseData.set("permissions", permissions);
        sendSuccessResponse(response, responseData);
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

    private boolean canUserEditAbbreviation(User user, Abbreviation abbreviation) {
        return user.isAdmin() || user.getId() == abbreviation.getUserId();
    }

    private boolean canUserDeleteAbbreviation(User user, Abbreviation abbreviation) {
        return user.isAdmin() || user.getId() == abbreviation.getUserId();
    }

    private ObjectNode abbreviationToJson(Abbreviation abbreviation, User currentUser) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("id", abbreviation.getId());
        json.put("name", abbreviation.getName());
        json.put("description", abbreviation.getDescription());
        json.put("views", abbreviation.getViews());
        json.put("likes", abbreviation.getLikes());
        json.put("favorites", abbreviation.getFavorites());
        json.put("createdAt", abbreviation.getCreatedAt().toString());

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

        if (abbreviation.getUser() != null) {
            ObjectNode authorJson = objectMapper.createObjectNode();
            authorJson.put("username", abbreviation.getUser().getUsername());
            json.set("author", authorJson);
        }

        json.put("contextKey", abbreviation.getName() + "-" +
                (abbreviation.getLanguage() != null ? abbreviation.getLanguage().getCode() : "") + "-" +
                (abbreviation.getDomain() != null ? abbreviation.getDomain().getCode() : ""));
        json.put("contextDescription",
                (abbreviation.getLanguage() != null ? abbreviation.getLanguage().getName() : "Unknown") +
                        " / " +
                        (abbreviation.getDomain() != null ? abbreviation.getDomain().getName() : "Unknown"));

        if (currentUser != null) {
            try {
                json.put("isLiked", abbreviationDAO.userHasLiked(abbreviation.getId(), currentUser.getId()));
                json.put("isFavorited", abbreviationDAO.userHasFavorited(abbreviation.getId(), currentUser.getId()));
                json.put("canEdit", canUserEditAbbreviation(currentUser, abbreviation));
                json.put("canDelete", canUserDeleteAbbreviation(currentUser, abbreviation));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return json;
    }

    private String generateDocBook(String name, List<String> meanings, String description) {
        StringBuilder docbook = new StringBuilder();
        docbook.append("<article xmlns=\"http://docbook.org/ns/docbook\">\n");
        docbook.append("    <title>").append(name).append("</title>\n");

        if (description != null && !description.trim().isEmpty()) {
            docbook.append("    <para>").append(description).append("</para>\n");
        }

        docbook.append("    <variablelist>\n");
        for (String meaning : meanings) {
            docbook.append("        <varlistentry>\n");
            docbook.append("            <term>Semnificație</term>\n");
            docbook.append("            <listitem><para>").append(meaning).append("</para></listitem>\n");
            docbook.append("        </varlistentry>\n");
        }
        docbook.append("    </variablelist>\n");
        docbook.append("</article>");

        return docbook.toString();
    }

    private int getLanguageId(String code) {
        switch (code.toLowerCase()) {
            case "ro": return 1;
            case "en": return 2;
            case "fr": return 3;
            case "de": return 4;
            default: return -1;
        }
    }

    private int getDomainId(String code) {
        switch (code.toLowerCase()) {
            case "tech": return 1;
            case "medical": return 2;
            case "business": return 3;
            case "science": return 4;
            case "general": return 5;
            default: return -1;
        }
    }

    private List<Abbreviation> getFilteredAbbreviations(String filter, User currentUser, int limit, int offset) throws SQLException {
        switch (filter) {
            case "mine":
                return abbreviationDAO.getUserAbbreviations(currentUser.getId(), limit, offset);
            case "favorites":
                return abbreviationDAO.getUserFavorites(currentUser.getId(), limit, offset);
            case "all":
            default:
                return abbreviationDAO.getAll(limit, offset);
        }
    }

    private List<Abbreviation> applyPagination(List<Abbreviation> abbreviations, int limit, int offset) {
        int start = Math.min(offset, abbreviations.size());
        int end = Math.min(start + limit, abbreviations.size());
        return abbreviations.subList(start, end);
    }

    private String getRequiredString(JsonNode node, String fieldName) throws IOException {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            throw new IOException("Câmpul '" + fieldName + "' este obligatoriu");
        }
        String value = node.get(fieldName).asText().trim();
        if (value.isEmpty()) {
            throw new IOException("Câmpul '" + fieldName + "' nu poate fi gol");
        }
        return value;
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