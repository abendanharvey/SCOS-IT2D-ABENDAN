
package com.schoolcanteen.ui;

import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, Scanner scanner, User currentAdmin) {
        this.userDao = userDao;
        this.scanner = scanner;
        this.currentAdmin = currentAdmin;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== ADMIN MENU ==========");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User Role and Status");
            System.out.println("0. Logout");
            System.out.println("================================");

            int choice = readIntInput("Enter choice: ");

            switch (choice) {
                case 1 -> viewAllUsers();
                case 2 -> viewPendingUsers();
                case 3 -> approveUser();
                case 4 -> updateUser();
                case 0 -> {
                    System.out.println("Logging out...");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void viewAllUsers() {
        try {
            List<User> users = userDao.getAllUsers();
            displayUsers(users);
        } catch (SQLException e) {
            System.out.println("Unable to retrieve users.");
            e.printStackTrace();
        }
    }

    private void viewPendingUsers() {
        try {
            List<User> users = userDao.getPendingUsers();
            displayUsers(users);
        } catch (SQLException e) {
            System.out.println("Unable to retrieve pending users.");
            e.printStackTrace();
        }
    }

    private void approveUser() {
        try {
            List<User> pendingUsers = userDao.getPendingUsers();

            if (pendingUsers.isEmpty()) {
                System.out.println("No pending registrations found.");
                return;
            }

            displayUsers(pendingUsers);

            int userId = readIntInput("Enter User ID to approve (0 to cancel): ");

            if (userId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            boolean found = pendingUsers.stream()
                    .anyMatch(user -> user.userId() == userId);

            if (!found) {
                System.out.println("User ID not found in pending registrations.");
                return;
            }

            if (userDao.approveUser(userId)) {
                System.out.println("User account approved successfully.");
            } else {
                System.out.println("Unable to approve user account.");
            }

        } catch (SQLException e) {
            System.out.println("Error approving user.");
            e.printStackTrace();
        }
    }

    private void updateUser() {
        try {
            List<User> users = userDao.getAllUsers();

            if (users.isEmpty()) {
                System.out.println("No users found.");
                return;
            }

            displayUsers(users);

            int userId = readIntInput(
                    "Enter User ID to update (0 to cancel): "
            );

            if (userId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            if (userId == currentAdmin.userId()) {
                System.out.println(
                        "WARNING: Modifying the currently active admin account is restricted."
                );
                return;
            }

            boolean userExists = users.stream()
                    .anyMatch(user -> user.userId() == userId);

            if (!userExists) {
                System.out.println("User ID not found.");
                return;
            }

            String newRole = readValidRole();

            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }

            String newStatus = readValidStatus();

            if (newStatus == null) {
                System.out.println("Update cancelled.");
                return;
            }

            boolean updated = userDao.updateUserRoleAndStatus(
                    userId,
                    newRole,
                    newStatus
            );

            if (updated) {
                System.out.println("User role and status updated successfully.");
            } else {
                System.out.println("No user was updated.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating user.");
            e.printStackTrace();
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.println("\nAvailable Roles:");
            System.out.println("1. STUDENT");
            System.out.println("2. CANTEEN_STAFF");
            System.out.println("3. ADMIN");
            System.out.println("0. Cancel");

            int choice = readIntInput("Choose new role: ");

            if (choice == 0) {
                return null;
            }

            switch (choice) {
                case 1:
                    return "STUDENT";
                case 2:
                    return "CANTEEN_STAFF";
                case 3:
                    return "ADMIN";
                default:
                    System.out.println("Invalid role choice. Please try again.");
            }
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.println("\nAvailable Account Statuses:");
            System.out.println("1. APPROVED");
            System.out.println("2. PENDING");
            System.out.println("0. Cancel");

            int choice = readIntInput("Choose new status: ");

            if (choice == 0) {
                return null;
            }

            switch (choice) {
                case 1:
                    return "APPROVED";
                case 2:
                    return "PENDING";
                default:
                    System.out.println("Invalid status choice. Please try again.");
            }
        }
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(
                        "Invalid input. Please enter a number."
                );
            }
        }
    }

    private void displayUsers(List<User> users) {
        System.out.println("\n================ SYSTEM USERS ================");
        System.out.printf(
                "%-8s %-20s %-18s %-15s%n",
                "ID", "USERNAME", "ROLE", "STATUS"
        );
        System.out.println("------------------------------------------------");

        for (User user : users) {
            System.out.printf(
                    "%-8d %-20s %-18s %-15s%n",
                    user.userId(),
                    shorten(user.username(), 19),
                    user.role(),
                    user.accountStatus()
            );
        }

        System.out.println("================================================");
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }

        return value;
    }
}
