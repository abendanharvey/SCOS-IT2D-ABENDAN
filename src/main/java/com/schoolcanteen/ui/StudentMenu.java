package com.schoolcanteen.ui;

import com.schoolcanteen.dao.OrderDao;

import java.sql.SQLException;
import java.util.Scanner;

public class StudentMenu {

    private final Scanner scanner;
    private final int userId;
    private final OrderDao orderDao;

    public StudentMenu(Scanner scanner, int userId) {
        this.scanner = scanner;
        this.userId = userId;
        this.orderDao = new OrderDao();
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== STUDENT MENU ==========");
            System.out.println("1. Order Food");
            System.out.println("2. View My Pending Orders");
            System.out.println("3. View Order History");
            System.out.println("0. Logout");
            System.out.println("=================================");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> orderFood();
                case "2" -> viewMyPendingOrders();
                case "3" -> viewOrderHistory();
                case "0" -> {
                    System.out.println("Logging out...");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // ORDER FOOD
    private void orderFood() {

        System.out.println("\n========== AVAILABLE DISHES ==========");
        System.out.println("1. Fried Chicken       - PHP 50.00");
        System.out.println("2. Pork Adobo          - PHP 45.00");
        System.out.println("3. Pancit              - PHP 35.00");
        System.out.println("4. Siomai Rice         - PHP 40.00");
        System.out.println("5. Burger              - PHP 35.00");
        System.out.println("6. Hotdog Sandwich     - PHP 30.00");
        System.out.println("7. Fries               - PHP 25.00");
        System.out.println("8. Bottled Water       - PHP 15.00");
        System.out.println("0. Back");
        System.out.println("=====================================");

        System.out.print("Enter dish number: ");
        String dishChoice = scanner.nextLine().trim();

        if (dishChoice.equals("0")) {
            return;
        }

        int foodId;
        double price;
        String dishName;

        switch (dishChoice) {
            case "1" -> {
                foodId = 1;
                dishName = "Fried Chicken";
                price = 50.00;
            }
            case "2" -> {
                foodId = 2;
                dishName = "Pork Adobo";
                price = 45.00;
            }
            case "3" -> {
                foodId = 3;
                dishName = "Pancit";
                price = 35.00;
            }
            case "4" -> {
                foodId = 4;
                dishName = "Siomai Rice";
                price = 40.00;
            }
            case "5" -> {
                foodId = 5;
                dishName = "Burger";
                price = 35.00;
            }
            case "6" -> {
                foodId = 6;
                dishName = "Hotdog Sandwich";
                price = 30.00;
            }
            case "7" -> {
                foodId = 7;
                dishName = "Fries";
                price = 25.00;
            }
            case "8" -> {
                foodId = 8;
                dishName = "Bottled Water";
                price = 15.00;
            }
            default -> {
                System.out.println("Invalid dish selection.");
                return;
            }
        }

        System.out.print("Enter quantity: ");
        String quantityInput = scanner.nextLine().trim();

        try {
            int quantity = Integer.parseInt(quantityInput);

            if (quantity <= 0) {
                System.out.println("Quantity must be greater than zero.");
                return;
            }

            double total = price * quantity;

            System.out.println("\n========== ORDER SUMMARY ==========");
            System.out.println("Dish: " + dishName);
            System.out.println("Price: PHP " + price);
            System.out.println("Quantity: " + quantity);
            System.out.println("Total: PHP " + total);
            System.out.println("=================================");

            System.out.print("Confirm order? (Y/N): ");
            String confirmation = scanner.nextLine().trim();

            if (!confirmation.equalsIgnoreCase("Y")) {
                System.out.println("Order cancelled.");
                return;
            }

            int orderId = orderDao.placeOrder(userId, foodId, quantity);

            System.out.println("\nOrder placed successfully!");
            System.out.println("Order ID: " + orderId);
            System.out.println("Status: PENDING");
            System.out.println("Please wait for canteen staff approval.");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric quantity.");

        } catch (SQLException e) {
            System.out.println("Failed to place order.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // VIEW PENDING ORDERS
    private void viewMyPendingOrders() {
        try {
            orderDao.viewMyOrders(userId);
        } catch (SQLException e) {
            System.out.println("Failed to retrieve pending orders.");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // VIEW ORDER HISTORY
    private void viewOrderHistory() {
        try {
            orderDao.viewOrderHistory(userId);
        } catch (SQLException e) {
            System.out.println("Failed to retrieve order history.");
            System.out.println("Reason: " + e.getMessage());
        }
    }
}