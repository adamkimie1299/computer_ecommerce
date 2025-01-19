// CartServlet.java
package com.shop.ecommerce.servlet;

import com.google.gson.Gson;
import com.shop.ecommerce.dao.CartDAO;
import com.shop.ecommerce.model.Cart;
import com.shop.ecommerce.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/cart/*")
public class CartServlet extends HttpServlet {
    private final CartDAO cartDAO = new CartDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");

        try {
            Cart cart = cartDAO.getOrCreateCart(user.getId());
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(cart));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error fetching cart");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");

        String pathInfo = request.getPathInfo();
        try {
            if ("/add".equals(pathInfo)) {
                handleAddToCart(request, response, user);
            } else if ("/update".equals(pathInfo)) {
                handleUpdateCart(request, response, user);
            } else if ("/remove".equals(pathInfo)) {
                handleRemoveFromCart(request, response, user);
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Cart operation failed");
        }
    }

    private void handleAddToCart(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        int productId = Integer.parseInt(request.getParameter("productId"));
        int quantity = Integer.parseInt(request.getParameter("quantity"));

        Cart cart = cartDAO.getOrCreateCart(user.getId());
        cartDAO.addToCart(cart.getId(), productId, quantity);

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(cart));
    }

    private void handleUpdateCart(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        int productId = Integer.parseInt(request.getParameter("productId"));
        int quantity = Integer.parseInt(request.getParameter("quantity"));

        Cart cart = cartDAO.getOrCreateCart(user.getId());
        cartDAO.updateCartItemQuantity(cart.getId(), productId, quantity);

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(cart));
    }

    private void handleRemoveFromCart(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        int productId = Integer.parseInt(request.getParameter("productId"));

        Cart cart = cartDAO.getOrCreateCart(user.getId());
        cartDAO.removeFromCart(cart.getId(), productId);

        response.setContentType("application/json");
        response.getWriter().write(gson.toJson(cart));
    }


}