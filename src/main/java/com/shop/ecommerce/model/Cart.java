//Cart.java
package com.shop.ecommerce.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Cart {
    private int id;
    private int userId;
    private Timestamp createdAt;
    private List<CartItem> items;

    public Cart() {
        this.items = new ArrayList<>();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    // Calculate total price of the items in the cart
    public double getTotalPrice() {
        double totalPrice = 0;
        for (CartItem cartItem : items) {  // Iterate over CartItem objects
            totalPrice += cartItem.getProduct().getPrice() * cartItem.getQuantity();  // Price * Quantity for each item
        }
        return totalPrice;
    }
}
