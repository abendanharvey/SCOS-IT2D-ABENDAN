package com.schoolcanteen.ui;

import com.schoolcanteen.dao.OrderDao;

import java.sql.SQLException;
import java.util.Scanner;

public class CanteenStaffMenu {

    private final Scanner scanner;
    private final int userId;
    private final OrderDao orderDao;

    public CanteenStaffMenu(Scanner scanner, int userId) {
        this.scanner = scanner;
        this.userId = userId;
        this.orderDao = new OrderDao();
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== CANTEEN STAFF DASHBOARD ==========");
            System.out.println("1. View Pending Orders");
            System.out.println("2. Approve Order");
            System.out.println("3. View Food Stocks");
            System.out.println("4. Restock Food");
            System.out.println("0. Logout");
            System.out.println("=============================================");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewPendingOrders();
                case "2" -> approveOrder();
                case "3" -> viewFoodStocks();
                case "4" -> restockFood();
                case "0" -> {
                    System.out.println("Logging out...");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // VIEW PENDING ORDERS
    private void viewPendingOrders() {
        try {
            orderDao.viewPendingOrders();
        } catch (SQLException e) {
            System.out.println("Failed to retrieve pending orders.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // APPROVE ORDER
    private void approveOrder() {
        System.out.println("\n========== APPROVE ORDER ==========");

        System.out.print("Enter Order ID to approve (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.equals("0")) {
            return;
        }

        try {
            int orderId = Integer.parseInt(input);

            if (orderId <= 0) {
                System.out.println("Order ID must be greater than zero.");
                return;
            }

            boolean updated = orderDao.updateOrderStatus(
                    userId,
                    orderId,
                    "APPROVED"
            );

            if (updated) {
                System.out.println("Order #" + orderId + " approved successfully!");
                System.out.println("Food stock has been deducted.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric Order ID.");

        } catch (SQLException e) {
            System.out.println("Failed to approve order.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // VIEW FOOD STOCKS
    private void viewFoodStocks() {
        try {
            orderDao.viewFoodStocks();
        } catch (SQLException e) {
            System.out.println("Failed to retrieve food stocks.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // RESTOCK FOOD
    private void restockFood() {

        System.out.println("\n========== RESTOCK FOOD ==========");

        try {
            orderDao.viewFoodStocks();

            System.out.print("\nEnter Food ID to restock (0 to cancel): ");
            String foodInput = scanner.nextLine().trim();

            if (foodInput.equals("0")) {
                return;
            }

            int foodId = Integer.parseInt(foodInput);

            if (foodId <= 0) {
                System.out.println("Food ID must be greater than zero.");
                return;
            }

            System.out.print("Enter quantity to add (0 to cancel): ");
            String quantityInput = scanner.nextLine().trim();

            if (quantityInput.equals("0")) {
                return;
            }

            int quantity = Integer.parseInt(quantityInput);

            if (quantity <= 0) {
                System.out.println("Quantity must be greater than zero.");
                return;
            }

            boolean success = orderDao.restockFood(
                    foodId,
                    quantity,
                    userId
            );

            if (success) {
                System.out.println("Stock successfully added!");
                System.out.println("Food ID: " + foodId);
                System.out.println("Added Quantity: " + quantity);

                System.out.println("\nUpdated food stocks:");
                orderDao.viewFoodStocks();

            } else {
                System.out.println("Food item not found.");
            }

        } catch (NumberFormatException e) {
            System.out.println("Please enter valid numeric values.");

        } catch (SQLException e) {
            System.out.println("Failed to restock food.");
            System.out.println("Reason: " + e.getMessage());
        }
    }
}