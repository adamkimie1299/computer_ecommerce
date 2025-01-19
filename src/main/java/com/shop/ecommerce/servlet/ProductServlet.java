//ProductServlet.java
package com.shop.ecommerce.servlet;

import com.shop.ecommerce.dao.ProductDAO;
import com.shop.ecommerce.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

@WebServlet("/api/products")
public class ProductServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ProductServlet.class.getName());
    private final ProductDAO productDAO = new ProductDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            List<Product> products = productDAO.getAllProducts();
            JSONArray jsonArray = new JSONArray();
            String contextPath = request.getContextPath();

            for (Product product : products) {
                JSONObject json = new JSONObject();
                json.put("id", product.getId());
                json.put("name", product.getName());
                json.put("description", product.getDescription());
                json.put("price", product.getPrice());
                json.put("category", product.getCategory());
                json.put("stock", product.getStock());

                // Handle image URL
                String imageUrl;
                if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                    // Use the stored image URL
                    imageUrl = contextPath + "/" + product.getImageUrl();
                } else {
                    // Fallback to placeholder if no image URL is stored
                    imageUrl = contextPath + "/images/placeholder.jpg";
                }
                json.put("imageUrl", imageUrl);

                jsonArray.put(json);
            }
            response.getWriter().write(jsonArray.toString());

        } catch (SQLException e) {
            LOGGER.severe("Error fetching products: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JSONObject error = new JSONObject();
            error.put("error", "Failed to fetch products");
            response.getWriter().write(error.toString());
        }
    }
}