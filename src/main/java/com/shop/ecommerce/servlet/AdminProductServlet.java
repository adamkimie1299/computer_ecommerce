//AdminProductServlet
package com.shop.ecommerce.servlet;

import com.google.gson.Gson;
import com.shop.ecommerce.dao.ProductDAO;
import com.shop.ecommerce.model.Product;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

@WebServlet("/api/admin/products/*")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,    // 1 MB
        maxFileSize = 1024 * 1024 * 10,      // 10 MB
        maxRequestSize = 1024 * 1024 * 15    // 15 MB
)
public class AdminProductServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AdminProductServlet.class.getName());
    private final ProductDAO productDAO = new ProductDAO();
    private final Gson gson = new Gson();
    private static final String UPLOAD_DIRECTORY = "images";

    @Override
    public void init() throws ServletException {
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
        Path uploadDir = Paths.get(uploadPath);

        if (!Files.exists(uploadDir)) {
            try {
                Files.createDirectories(uploadDir);
                LOGGER.info("Created upload directory: " + uploadPath);
            } catch (IOException e) {
                LOGGER.severe("Failed to create upload directory: " + e.getMessage());
                throw new ServletException("Failed to create upload directory", e);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List all products
                String json = gson.toJson(productDAO.getAllProducts());
                response.getWriter().write(json);
            } else {
                // Get single product
                int productId = Integer.parseInt(pathInfo.substring(1));
                Product product = productDAO.getProductById(productId);
                if (product != null) {
                    String json = gson.toJson(product);
                    response.getWriter().write(json);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Extract form fields
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            double price = Double.parseDouble(request.getParameter("price"));
            int stock = Integer.parseInt(request.getParameter("stock"));
            String category = request.getParameter("category");

            // Handle file upload
            String imageUrl = null;
            Part filePart = request.getPart("image");
            if (filePart != null && filePart.getSize() > 0) {
                imageUrl = handleFileUpload(filePart);
            }

            // Create product object
            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(stock);
            product.setCategory(category);
            product.setImageUrl(imageUrl);

            // Save to database
            product = productDAO.createProduct(product);

            // Send response
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(gson.toJson(product));

        } catch (Exception e) {
            LOGGER.severe("Error creating product: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error creating product: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int productId = Integer.parseInt(pathInfo.substring(1));

            // Get existing product
            Product product = productDAO.getProductById(productId);
            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Update fields
            String name = request.getParameter("name");
            String description = request.getParameter("description");
            double price = Double.parseDouble(request.getParameter("price"));
            int stock = Integer.parseInt(request.getParameter("stock"));
            String category = request.getParameter("category");

            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(stock);
            product.setCategory(category);

            // Handle file upload
            Part filePart = request.getPart("image");
            if (filePart != null && filePart.getSize() > 0) {
                // Delete old image if it exists
                if (product.getImageUrl() != null) {
                    deleteImage(product.getImageUrl());
                }
                String newImageUrl = handleFileUpload(filePart);
                product.setImageUrl(newImageUrl);
            }

            // Save updated product
            if (productDAO.updateProduct(product)) {
                response.setContentType("application/json");
                response.getWriter().write(gson.toJson(product));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            LOGGER.severe("Error updating product: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error updating product: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int productId = Integer.parseInt(pathInfo.substring(1));

            // Get product to delete its image
            Product product = productDAO.getProductById(productId);
            if (product != null && product.getImageUrl() != null) {
                deleteImage(product.getImageUrl());
            }

            if (productDAO.deleteProduct(productId)) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            LOGGER.severe("Database error during delete: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }

    private String handleFileUpload(Part filePart) throws IOException {
        String fileName = UUID.randomUUID().toString() + getFileExtension(filePart);
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;

        // Create images directory if it doesn't exist
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Save the file
        Path filePath = Paths.get(uploadPath, fileName);
        try (InputStream input = filePart.getInputStream()) {
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path
        return UPLOAD_DIRECTORY + "/" + fileName;
    }

    private void deleteImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                Path imagePath = Paths.get(getServletContext().getRealPath(""), imageUrl);
                Files.deleteIfExists(imagePath);
                LOGGER.info("Successfully deleted image: " + imageUrl);
            } catch (IOException e) {
                LOGGER.warning("Failed to delete image: " + e.getMessage());
            }
        }
    }

    private String getFileExtension(Part part) {
        String submittedFileName = part.getSubmittedFileName();
        return submittedFileName.substring(submittedFileName.lastIndexOf("."));
    }
}