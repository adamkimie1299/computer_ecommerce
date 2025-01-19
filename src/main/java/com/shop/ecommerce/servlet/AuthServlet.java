//AuthServlet
package com.shop.ecommerce.servlet;

import com.shop.ecommerce.dao.UserDAO;
import com.shop.ecommerce.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();

        switch (pathInfo) {
            case "/register":
                handleRegistration(request, response);
                break;
            case "/login":
                handleLogin(request, response);
                break;
            case "/logout":
                handleLogout(request, response);
                break;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        try {
            User user = userDAO.findByUsername(username);

            if (user != null && password.equals(user.getPassword())) {
                HttpSession session = request.getSession(true); // Create new session
                session.setAttribute("user", user);

                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");

                // Log the role for debugging
                String role = (user.getRole() != null) ? user.getRole().trim().toUpperCase() : "USER";
                LOGGER.info("Login successful for user: " + username + " with role: " + role);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(role);

            } else {
                LOGGER.warning("Failed login attempt for username: " + username);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid credentials");
            }
        } catch (SQLException e) {
            LOGGER.severe("Database error during login: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error");
        }
    }

    private void handleRegistration(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String email = request.getParameter("email");

        try {
            if (userDAO.findByUsername(username) != null) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("Username already exists");
                return;
            }

            User newUser = new User();
            newUser.setUsername(username);
            newUser.setPassword(password);
            newUser.setEmail(email);
            newUser.setRole("USER");  // Always set as uppercase

            userDAO.createUser(newUser);

            LOGGER.info("Registration successful for username: " + username);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Registration successful");

        } catch (SQLException e) {
            LOGGER.severe("Database error during registration: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error");
        }
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/login.html");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (user != null) {
            String role = (user.getRole() != null) ? user.getRole().trim().toUpperCase() : "USER";
            String json = String.format(
                    "{\"authenticated\":true,\"username\":\"%s\",\"role\":\"%s\"}",
                    user.getUsername(),
                    role
            );
            response.getWriter().write(json);
        } else {
            response.getWriter().write("{\"authenticated\":false}");
        }
    }
}