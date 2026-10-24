
package com.schoolcanteen.ui;

import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n====================================");
            System.out.println("       SCHOOL CANTEEN ADMIN MENU");
            System.out.println("====================================");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "0" -> {
                    System.out.println("Admin logged out.");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void viewAllUsers() {
        try {
            List<User> users = userDao.getAllUsers();
            displayUsers(users);
        } catch (SQLException e) {
            System.out.println("Error retrieving users: " + e.getMessage());
        }
    }

    private void viewPendingUsers() {
        try {
            List<User> users = userDao.getPendingUsers();
            displayUsers(users);
        } catch (SQLException e) {
            System.out.println("Error retrieving pending users: "
                    + e.getMessage());
        }
    }

    private void approveUser() {
        try {
            List<User> pendingUsers = userDao.getPendingUsers();

            if (pendingUsers.isEmpty()) {
                System.out.println("There are no pending accounts.");
                return;
            }

            System.out.println("\nPENDING USER ACCOUNTS");
            displayUsers(pendingUsers);

            System.out.print("\nEnter User ID to approve (0 to cancel): ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                System.out.println("Approval cancelled.");
                return;
            }

            int userId;

            try {
                userId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid User ID. Enter a number.");
                return;
            }

            boolean exists = pendingUsers.stream()
                    .anyMatch(user -> user.userId() == userId);

            if (!exists) {
                System.out.println(
                        "User ID not found in the pending accounts list.");
                return;
            }

            boolean approved = userDao.approveUser(userId);

            if (approved) {
                System.out.println(
                        "Account approved successfully! User ID: " + userId);
            } else {
                System.out.println(
                        "Approval failed. The account may no longer be pending.");
            }

        } catch (SQLException e) {
            System.out.println("Error approving account: " + e.getMessage());
        }
    }

    private void displayUsers(List<User> users) {
        System.out.println();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        String border = "-------------------------------------------------------------";

        System.out.println(border);
        System.out.printf("%-5s %-15s %-12s %-10s%n",
                "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);

        for (User user : users) {
            System.out.printf("%-5d %-15s %-12s %-10s%n",
                    user.userId(),
                    shorten(user.username(), 15),
                    shorten(user.role(), 12),
                    shorten(user.accountStatus(), 10));
        }

        System.out.println(border);
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength - 3) + "...";
    }
}
