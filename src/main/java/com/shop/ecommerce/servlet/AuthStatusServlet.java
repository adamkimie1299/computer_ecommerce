//AuthStatusServlet

package com.shop.ecommerce.servlet;

import com.shop.ecommerce.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import com.google.gson.Gson;
import java.io.IOException;

@WebServlet("/api/auth/status")
public class AuthStatusServlet extends HttpServlet {
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        response.setContentType("application/json");
        if (user != null) {
            // Create a simplified user object without sensitive information
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(gson.toJson(new UserDTO(user.getUsername(), user.getEmail(), user.getRole())));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{}");
        }
    }

    // DTO to avoid sending sensitive information
    private static class UserDTO {
        private String username;
        private String email;
        private String role;

        public UserDTO(String username, String email, String role) {
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}