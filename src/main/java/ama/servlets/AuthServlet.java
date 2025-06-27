package ama.servlets;

import ama.database.dao.UserDAO;
import ama.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {
    private UserDAO userDAO;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        this.userDAO = new UserDAO();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();

        if (pathInfo == null) {
            sendErrorResponse(response, 400, "Endpoint invalid");
            return;
        }

        setCORSHeaders(response);

        try {
            switch (pathInfo) {
                case "/login":
                    handleLogin(request, response);
                    break;
                case "/register":
                    handleRegister(request, response);
                    break;
                case "/logout":
                    handleLogout(request, response);
                    break;
                default:
                    sendErrorResponse(response, 404, "Endpoint nu există");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare internă a serverului");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String pathInfo = request.getPathInfo();
        setCORSHeaders(response);

        try {
            if ("/me".equals(pathInfo)) {
                handleGetCurrentUser(request, response);
            } else {
                sendErrorResponse(response, 404, "Endpoint nu există");
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

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        // ctim datele din request
        ObjectNode requestData = (ObjectNode) objectMapper.readTree(request.getReader());

        String username = requestData.get("username").asText();
        String password = requestData.get("password").asText();

        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Username și parola sunt obligatorii");
            return;
        }

        try {
            Optional<User> userOpt = userDAO.authenticate(username.trim());

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                String hashedPassword = hashPassword(password);

                if (user.getPassword().equals(hashedPassword) ||
                        checkPlainPassword(user.getPassword(), password)) {

                    String token = generateToken();

                    /// salvam sesiunea
                    HttpSession session = request.getSession();
                    session.setAttribute("user_id", user.getId());
                    session.setAttribute("token", token);
                    session.setMaxInactiveInterval(24 * 60 * 60); // 24 ore

                    ObjectNode responseData = objectMapper.createObjectNode();
                    responseData.put("success", true);
                    responseData.put("message", "Autentificare reușită");
                    responseData.put("token", token);

                    ObjectNode userData = objectMapper.createObjectNode();
                    userData.put("id", user.getId());
                    userData.put("username", user.getUsername());
                    userData.put("email", user.getEmail());
                    userData.put("role", user.getRole());
                    responseData.set("user", userData);

                    sendSuccessResponse(response, responseData);

                } else {
                    sendErrorResponse(response, 401, "Credențiale incorecte");
                }
            } else {
                sendErrorResponse(response, 401, "Credențiale incorecte");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la accesarea bazei de date");
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        ObjectNode requestData = (ObjectNode) objectMapper.readTree(request.getReader());

        String username = requestData.get("username").asText();
        String email = requestData.get("email").asText();
        String password = requestData.get("password").asText();

        if (username == null || username.trim().isEmpty() ||
                email == null || email.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            sendErrorResponse(response, 400, "Toate câmpurile sunt obligatorii");
            return;
        }

        if (username.length() < 3) {
            sendErrorResponse(response, 400, "Username-ul trebuie să aibă cel puțin 3 caractere");
            return;
        }

        if (password.length() < 6) {
            sendErrorResponse(response, 400, "Parola trebuie să aibă cel puțin 6 caractere");
            return;
        }

        if (!isValidEmail(email)) {
            sendErrorResponse(response, 400, "Email invalid");
            return;
        }

        try {
            if (userDAO.usernameExists(username.trim())) {
                sendErrorResponse(response, 409, "Username-ul este deja folosit");
                return;
            }

            if (userDAO.emailExists(email.trim())) {
                sendErrorResponse(response, 409, "Email-ul este deja folosit");
                return;
            }

            User newUser = new User();
            newUser.setUsername(username.trim());
            newUser.setEmail(email.trim());
            newUser.setPassword(hashPassword(password));
            newUser.setRole("user");

            User createdUser = userDAO.create(newUser);

            ObjectNode responseData = objectMapper.createObjectNode();
            responseData.put("success", true);
            responseData.put("message", "Contul a fost creat cu succes");

            ObjectNode userData = objectMapper.createObjectNode();
            userData.put("id", createdUser.getId());
            userData.put("username", createdUser.getUsername());
            userData.put("email", createdUser.getEmail());
            responseData.set("user", userData);

            sendSuccessResponse(response, responseData);

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la crearea contului");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ObjectNode responseData = objectMapper.createObjectNode();
        responseData.put("success", true);
        responseData.put("message", "Deconectare reușită");

        sendSuccessResponse(response, responseData);
    }

    private void handleGetCurrentUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            sendErrorResponse(response, 401, "Nu ești autentificat");
            return;
        }

        try {
            int userId = (Integer) session.getAttribute("user_id");
            Optional<User> userOpt = userDAO.findById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                ObjectNode responseData = objectMapper.createObjectNode();
                responseData.put("success", true);

                ObjectNode userData = objectMapper.createObjectNode();
                userData.put("id", user.getId());
                userData.put("username", user.getUsername());
                userData.put("email", user.getEmail());
                userData.put("role", user.getRole());
                responseData.set("user", userData);

                sendSuccessResponse(response, responseData);
            } else {
                sendErrorResponse(response, 401, "Utilizator invalid");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendErrorResponse(response, 500, "Eroare la încărcarea utilizatorului");
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

    private String generateToken() {
        return UUID.randomUUID().toString();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Eroare la hash-uirea parolei", e);
        }
    }

    private boolean checkPlainPassword(String storedPassword, String inputPassword) {
        return storedPassword.equals(inputPassword);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}