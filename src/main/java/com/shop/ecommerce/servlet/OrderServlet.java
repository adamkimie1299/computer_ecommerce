package com.shop.ecommerce.servlet;

import com.google.gson.Gson;
import com.shop.ecommerce.dao.CartDAO;
import com.shop.ecommerce.dao.OrderDAO;
import com.shop.ecommerce.dao.ProductDAO;
import com.shop.ecommerce.model.Cart;
import com.shop.ecommerce.model.Order;
import com.shop.ecommerce.model.OrderItem;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.model.CartItem;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@WebServlet("/api/orders/*")
public class OrderServlet extends HttpServlet {
    private final OrderDAO orderDAO = new OrderDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String pathInfo = request.getPathInfo();
        try {
            if ("/user".equals(pathInfo)) {
                // Get all orders for the current user
                List<Order> allOrders = orderDAO.getAllUserOrders(user.getId());
                response.setContentType("application/json");
                response.getWriter().write(gson.toJson(allOrders));
            } else {
                // Get specific order details
                int orderId = Integer.parseInt(pathInfo.substring(1));
                Order order = orderDAO.getOrderById(orderId);
                if (order != null && (order.getUserId() == user.getId() || "ADMIN".equals(user.getRole()))) {
                    response.setContentType("application/json");
                    response.getWriter().write(gson.toJson(order));
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if ("/create".equals(pathInfo)) {
            createOrder(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void createOrder(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            HttpSession session = request.getSession(false);
            User user = (User) session.getAttribute("user");
            Cart cart = cartDAO.getOrCreateCart(user.getId());
            if (cart.getItems().isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cart is empty");
                return;
            }

            ProductDAO productDAO = new ProductDAO();
            // Check stock for all items before creating order
            for (CartItem cartItem : cart.getItems()) {
                boolean stockUpdated = productDAO.updateStock(
                        cartItem.getProductId(),
                        cartItem.getQuantity());
                if (!stockUpdated) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(
                            "Insufficient stock for product: " +
                                    cartItem.getProduct().getName());
                    return;
                }
            }

            // Create new order
            Order order = new Order();
            order.setUserId(user.getId());
            order.setName(request.getParameter("name"));
            order.setAddress(request.getParameter("address"));
            order.setPhone(request.getParameter("phone"));
            order.setPaymentMethod(request.getParameter("paymentMethod"));
            order.setTotalAmount(cart.getTotalPrice());

            // Convert cart items to order items
            cart.getItems().forEach(cartItem -> {
                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(cartItem.getProductId());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getProduct().getPrice());
                order.getItems().add(orderItem);
            });

            // Save order
            Order savedOrder = orderDAO.createOrder(order);
            response.setContentType("application/json");
            response.getWriter().write(gson.toJson(savedOrder));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Failed to create order: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User user = (User) session.getAttribute("user");
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo != null && pathInfo.endsWith("/cancel")) {
                // Extract order ID from the path
                int orderId = Integer.parseInt(pathInfo.substring(1, pathInfo.length() - 7));
                // Cancel order using updateOrderStatus
                if (orderDAO.updateOrderStatus(orderId, "CANCELED")) {
                    Order canceledOrder = orderDAO.getOrderById(orderId);
                    response.getWriter().write(gson.toJson(canceledOrder));
                } else {
                    String errorJson = gson.toJson(new ErrorResponse(
                            "Cannot cancel order. It may have already been processed or does not exist."));
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write(errorJson);
                }
            } else {
                String errorJson = gson.toJson(new ErrorResponse("Invalid endpoint"));
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(errorJson);
            }
        } catch (SQLException e) {
            String errorJson = gson
                    .toJson(new ErrorResponse("Database error while canceling order: " + e.getMessage()));
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorJson);
        } catch (NumberFormatException e) {
            String errorJson = gson.toJson(new ErrorResponse("Invalid order ID"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(errorJson);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || !pathInfo.startsWith("/")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int orderId = Integer.parseInt(pathInfo.substring(1));
            HttpSession session = request.getSession(false);
            User user = (User) session.getAttribute("user");

            // Handle admin deletion
            if ("ADMIN".equals(user.getRole())) {
                if (orderDAO.deleteOrder(orderId)) {
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Order not found or not in canceled status");
                }
            }
            // Handle user hiding order
            else {
                if (orderDAO.hideOrderFromUser(orderId, user.getId())) {
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND,
                            "Order not found or not in canceled status");
                }
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error processing delete request: " + e.getMessage());
        }
    }

    // Add this helper class at the end of your OrderServlet class
    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}