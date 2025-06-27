package ama.servlets;

import ama.database.dao.DomainDAO;
import ama.models.Domain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/domains")
public class DomainServlet extends HttpServlet {
    private DomainDAO domainDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.domainDAO = new DomainDAO();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setCORSHeaders(response);

        try {
            List<Domain> domains = domainDAO.getAllDomains();

            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("success", true);

            ArrayNode domainsArray = objectMapper.createArrayNode();
            for (Domain domain : domains) {
                ObjectNode domainJson = objectMapper.createObjectNode();
                domainJson.put("id", domain.getId());
                domainJson.put("code", domain.getCode());
                domainJson.put("name", domain.getName());
                domainsArray.add(domainJson);
            }
            responseData.set("domains", domainsArray);

            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.put("count", domains.size());
            responseData.set("metadata", metadata);

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la încărcarea domeniilor: " + e.getMessage());
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