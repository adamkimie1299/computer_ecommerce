package com.shop.ecommerce.dao;

import com.shop.ecommerce.model.Order;
import com.shop.ecommerce.model.OrderItem;
import com.shop.ecommerce.dao.CartDAO;
import com.shop.ecommerce.util.DatabaseUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {
    private final CartDAO cartDAO = new CartDAO();
    private static final Logger LOGGER = Logger.getLogger(OrderDAO.class.getName());

    public Order createOrder(Order order) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            // Insert order
            String orderSql = "INSERT INTO orders (user_id, name, address, phone, payment_method, total_amount, status, visible_to_user) "
                    +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, true)";

            try (PreparedStatement stmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, order.getUserId());
                stmt.setString(2, order.getName());
                stmt.setString(3, order.getAddress());
                stmt.setString(4, order.getPhone());
                stmt.setString(5, order.getPaymentMethod());
                stmt.setDouble(6, order.getTotalAmount());
                stmt.setString(7, order.getStatus());
                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        order.setId(rs.getInt(1));
                    }
                }
            }

            // Insert order items
            String itemSql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (OrderItem item : order.getItems()) {
                    stmt.setInt(1, order.getId());
                    stmt.setInt(2, item.getProductId());
                    stmt.setInt(3, item.getQuantity());
                    stmt.setDouble(4, item.getPrice());
                    stmt.executeUpdate();
                }
            }

            // Clear the user's cart
            cartDAO.clearCart(cartDAO.getOrCreateCart(order.getUserId()).getId());
            conn.commit();
            return order;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<Order> getAllOrders() throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getId()));
                orders.add(order);
            }
        }
        return orders;
    }

    public List<Order> getAllUserOrders(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id = ? AND visible_to_user = true ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    order.setItems(getOrderItems(order.getId()));
                    orders.add(order);
                }
            }
        }
        return orders;
    }

    public Order getOrderById(int orderId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    order.setItems(getOrderItems(orderId));
                    return order;
                }
            }
        }
        return null;
    }

    public boolean updateOrderStatus(int orderId, String status) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            // Update order status - removed updated_at field
            String updateOrderSql = "UPDATE orders SET status = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateOrderSql)) {
                stmt.setString(1, status);
                stmt.setInt(2, orderId);

                int rowsUpdated = stmt.executeUpdate();

                // If status is being changed to CANCELED, restore product stock
                if (status.equals("CANCELED")) {
                    String restoreStockSql = "UPDATE products p " +
                            "JOIN order_items oi ON oi.product_id = p.id " +
                            "SET p.stock = p.stock + oi.quantity " +
                            "WHERE oi.order_id = ?";

                    try (PreparedStatement restoreStmt = conn.prepareStatement(restoreStockSql)) {
                        restoreStmt.setInt(1, orderId);
                        restoreStmt.executeUpdate();
                    }
                }

                conn.commit();
                return rowsUpdated > 0;
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.severe("Error rolling back transaction: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.severe("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    public boolean hideOrderFromUser(int orderId, int userId) throws SQLException {
        String sql = "UPDATE orders SET visible_to_user = false WHERE id = ? AND user_id = ? AND status = 'CANCELED'";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteOrder(int orderId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false);

            // First delete order items
            String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteItemsSql)) {
                stmt.setInt(1, orderId);
                stmt.executeUpdate();
            }

            // Then delete the order
            String deleteOrderSql = "DELETE FROM orders WHERE id = ? AND status = 'CANCELED'";
            try (PreparedStatement stmt = conn.prepareStatement(deleteOrderSql)) {
                stmt.setInt(1, orderId);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.severe("Error rolling back transaction: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.severe("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    private List<OrderItem> getOrderItems(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.*, p.name as product_name FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE order_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(rs.getInt("id"));
                    item.setOrderId(rs.getInt("order_id"));
                    item.setProductId(rs.getInt("product_id"));
                    item.setProductName(rs.getString("product_name"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getDouble("price"));
                    items.add(item);
                }
            }
        }
        return items;
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setUserId(rs.getInt("user_id"));
        order.setName(rs.getString("name"));
        order.setAddress(rs.getString("address"));
        order.setPhone(rs.getString("phone"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setTotalAmount(rs.getDouble("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        return order;
    }
}