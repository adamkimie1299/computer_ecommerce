//AuthFilter.java
package com.shop.ecommerce.filter;

import com.shop.ecommerce.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Allow access to static resources and authentication endpoints
        if (isPublicResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        User user = session != null ? (User) session.getAttribute("user") : null;

        // Check if user is authenticated
        if (user == null) {
            if (isApiRequest(path)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("Unauthorized");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
            }
            return;
        }

        // Check for admin-only resources
        if (isAdminResource(path) && !user.getRole().equals("ADMIN")) {
            if (isApiRequest(path)) {
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.getWriter().write("Admin access required");
            } else {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.html");
            }
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicResource(String path) {
        return path.endsWith(".html") && (
                path.endsWith("/login.html") ||
                        path.endsWith("/register.html") ||
                        path.endsWith("/index.html")
        ) || path.contains("/auth/login") ||
                path.contains("/auth/register") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".ico") ||
                path.endsWith(".jpg") ||
                path.endsWith(".png");
    }

    private boolean isAdminResource(String path) {
        return path.contains("/api/admin/") ||
                path.endsWith("/admin.html");
    }

    private boolean isApiRequest(String path) {
        return path.startsWith("/api/");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code if needed
    }

    @Override
    public void destroy() {
        // Cleanup code if needed
    }
}