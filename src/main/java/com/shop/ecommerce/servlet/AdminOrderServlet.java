//AdminOrderServlet
package com.shop.ecommerce.servlet;

import com.google.gson.Gson;
import com.shop.ecommerce.dao.OrderDAO;
import com.shop.ecommerce.model.Order;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

@WebServlet("/api/admin/orders/*")
public class AdminOrderServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AdminOrderServlet.class.getName());
    private final OrderDAO orderDAO = new OrderDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List all orders
                String json = gson.toJson(orderDAO.getAllOrders());
                response.getWriter().write(json);
            } else {
                // Get single order
                int orderId = Integer.parseInt(pathInfo.substring(1));
                Order order = orderDAO.getOrderById(orderId);
                if (order != null) {
                    String json = gson.toJson(order);
                    response.getWriter().write(json);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write(gson.toJson(new ErrorResponse("Order not found")));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Database error: " + e.getMessage())));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid request")));
            return;
        }

        try {
            int orderId = Integer.parseInt(pathInfo.substring(1));
            String status = null;

            // Check for JSON payload first
            if ("application/json".equals(request.getContentType())) {
                StringBuilder sb = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                if (!sb.toString().isEmpty()) {
                    JsonObject jsonObject = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    status = jsonObject.get("status").getAsString();
                }
            }

            // If not JSON, try form parameter
            if (status == null) {
                status = request.getParameter("status");
            }

            if (status == null || status.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(gson.toJson(new ErrorResponse("Status is required")));
                return;
            }

            if (orderDAO.updateOrderStatus(orderId, status.toUpperCase())) {
                Order updatedOrder = orderDAO.getOrderById(orderId);
                response.getWriter().write(gson.toJson(updatedOrder));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("Order not found")));
            }
        } catch (SQLException e) {
            LOGGER.severe("Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Database error: " + e.getMessage())));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || !pathInfo.startsWith("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid order ID")));
            return;
        }

        try {
            int orderId = Integer.parseInt(pathInfo.substring(1));
            if (orderDAO.deleteOrder(orderId)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(gson.toJson(new SuccessResponse("Order deleted successfully")));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(gson.toJson(new ErrorResponse("Order not found or not in canceled status")));
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson(new ErrorResponse("Invalid order ID format")));
        } catch (SQLException e) {
            LOGGER.severe("Database error while deleting order: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(gson.toJson(new ErrorResponse("Database error: " + e.getMessage())));
        }
    }

    // Helper classes for JSON responses
    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }

    private static class SuccessResponse {
        private final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}