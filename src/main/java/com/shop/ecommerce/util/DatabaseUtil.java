//DatabaseUtil.java
package com.shop.ecommerce.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());

    // Database connection constants
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";  // Replace with your MySQL password

    // Initialize MySQL Driver
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("MySQL JDBC Driver registration failed.");
            throw new RuntimeException("Failed to register MySQL JDBC Driver", e);
        }
    }

    /**
     * Gets a connection to the database.
     *
     * @return A connection to the database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            if (connection != null) {
                LOGGER.fine("Database connection established successfully.");
                return connection;
            } else {
                LOGGER.severe("Failed to establish database connection.");
                throw new SQLException("Failed to establish database connection.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection error: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Closes a database connection safely.
     *
     * @param connection The connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.fine("Database connection closed successfully.");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Tests the database connection.
     *
     * @return true if connection is successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Main method to test the database connection.
     */
    public static void main(String[] args) {
        if (testConnection()) {
            System.out.println("Database connection test successful!");
        } else {
            System.out.println("Database connection test failed!");
        }
    }
}