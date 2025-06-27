package ama.servlets;

import ama.database.dao.AbbreviationDAO;
import ama.models.Abbreviation;
import ama.utils.DocBookExporter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Optional;

@WebServlet("/api/export/*")
public class ExportServlet extends HttpServlet {
    private AbbreviationDAO abbreviationDAO;
    private DocBookExporter docBookExporter;

    @Override
    public void init() throws ServletException {
        super.init();
        this.abbreviationDAO = new AbbreviationDAO();
        this.docBookExporter = new DocBookExporter();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        setCORSHeaders(response);

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.matches("/abbreviation/\\d+/\\w+")) {
                // GET /api/export/abbreviation/{id}/{format}
                String[] pathParts = pathInfo.split("/");
                int abbreviationId = Integer.parseInt(pathParts[2]);
                String format = pathParts[3].toLowerCase();

                handleExportAbbreviation(request, response, abbreviationId, format);
            } else {
                sendErrorResponse(response, 404, "Endpoint de export nu există");
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

    private void handleExportAbbreviation(HttpServletRequest request, HttpServletResponse response,
                                          int abbreviationId, String format) throws IOException, SQLException {

        Optional<Abbreviation> abbreviationOpt = abbreviationDAO.findById(abbreviationId);
        if (!abbreviationOpt.isPresent()) {
            sendErrorResponse(response, 404, "Abrevierea nu a fost găsită");
            return;
        }

        Abbreviation abbreviation = abbreviationOpt.get();

        // trebuie sa verificam ca are continut docbook
        if (abbreviation.getDocbook() == null || abbreviation.getDocbook().trim().isEmpty()) {
            sendErrorResponse(response, 404, "Nu există conținut DocBook pentru această abreviere");
            return;
        }

        try {
            String exportedContent;
            String contentType;
            String fileExtension;
            String filename;

            switch (format) {
                case "markdown":
                case "md":
                    exportedContent = docBookExporter.toMarkdown(abbreviation);
                    contentType = "text/markdown; charset=UTF-8";
                    fileExtension = "md";
                    break;

                case "html":
                    exportedContent = docBookExporter.toHTML(abbreviation);
                    contentType = "text/html; charset=UTF-8";
                    fileExtension = "html";
                    break;

                default:
                    sendErrorResponse(response, 400, "Format de export nesuportat: " + format);
                    return;
            }

            filename = sanitizeFilename(abbreviation.getName()) + "_export." + fileExtension;

            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.setHeader("Cache-Control", "no-cache");

            PrintWriter writer = response.getWriter();
            writer.write(exportedContent);
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la exportul în format " + format);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "abbreviation";
        }

        return filename.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_{2,}", "_") // Înlocuiește multiple underscores cu unul singur
                .replaceAll("^_|_$", ""); // Elimină underscores de la început și sfârșit
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
}