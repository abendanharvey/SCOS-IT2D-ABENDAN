package com.schoolcanteen.ui;

import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.dao.ActivityLogDao;
import com.schoolcanteen.model.User;
import com.schoolcanteen.model.ActivityLog;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;
    private final User currentAdmin;

    public AdminMenu(UserDao userDao, Scanner scanner, User currentAdmin) {
        this.userDao = userDao;
        this.activityLogDao = new ActivityLogDao();
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
            System.out.println("5. View System Audit Logs");
            System.out.println("0. Logout");
            System.out.println("================================");

            int choice = readIntInput("Enter choice: ");

            switch (choice) {
                case 1 -> viewAllUsers();
                case 2 -> viewPendingUsers();
                case 3 -> approveUser();
                case 4 -> updateUser();
                case 5 -> viewAuditLogs();
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

            boolean approved = userDao.approveUser(
                    userId,
                    currentAdmin.userId()
            );

            if (approved) {
                System.out.println("User account approved successfully.");
                System.out.println("Activity logged successfully.");
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
                    newStatus,
                    currentAdmin.userId()
            );

            if (updated) {
                System.out.println("User role and status updated successfully.");
                System.out.println("Activity logged successfully.");
            } else {
                System.out.println("No user was updated.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating user.");
            e.printStackTrace();
        }
    }

    // Audit log menu.

    private void viewAuditLogs() {
        boolean viewing = true;

        while (viewing) {
            System.out.println("\n======= SYSTEM AUDIT LOGS =======");
            System.out.println("1. View All Activity Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Activity Logs");
            System.out.println("0. Back to Admin Menu");
            System.out.println("=================================");

            int choice = readIntInput("Enter choice: ");

            switch (choice) {
                case 1 -> displayAuditLogs();
                case 2 -> filterLogsByUserId();
                case 3 -> clearAuditLogs();
                case 0 -> viewing = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // Clear historical logs with administrator confirmation.
    private void clearAuditLogs() {

        System.out.println("\nWARNING: This will delete all previous activity logs.");
        System.out.println("A new LOGS_CLEARED record will be saved.");

        System.out.print("Type YES to confirm or NO to cancel: ");
        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("YES")) {
            System.out.println("Log clearing cancelled.");
            return;
        }

        try {
            boolean cleared = activityLogDao.clearAllLogs(
                    currentAdmin.userId()
            );

            if (cleared) {
                System.out.println("Historical activity logs cleared successfully.");
                System.out.println("The clearing action has been recorded.");
            } else {
                System.out.println("Unable to clear activity logs.");
            }

        } catch (SQLException e) {
            System.out.println("Error clearing activity logs.");
            e.printStackTrace();
        }
    }


    // Display all system logs.
    private void displayAuditLogs() {
        try {
            List<ActivityLog> logs = activityLogDao.getAllLogs();
            displayLogs(logs);

        } catch (SQLException e) {
            System.out.println("Unable to retrieve activity logs.");
            e.printStackTrace();
        }
    }

    // Filter logs using a specific user ID.
    private void filterLogsByUserId() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel): ");

        if (userId == 0) {
            System.out.println("Log filtering cancelled.");
            return;
        }

        try {
            List<ActivityLog> logs =
                    activityLogDao.getLogsByUserId(userId);

            System.out.println("\nShowing logs for User ID: " + userId);
            displayLogs(logs);

        } catch (SQLException e) {
            System.out.println("Unable to filter activity logs.");
            e.printStackTrace();
        }
    }

    // Display logs in a formatted table.
    private void displayLogs(List<ActivityLog> logs) {

        if (logs.isEmpty()) {
            System.out.println("\nNo activity logs found.");
            return;
        }

        System.out.println("\n========================== SYSTEM AUDIT LOGS ==========================");

        System.out.printf(
                "%-5s | %-8s | %-18s | %-35s | %-20s%n",
                "LOG ID",
                "USER ID",
                "EVENT TYPE",
                "DESCRIPTION",
                "TIMESTAMP"
        );

        System.out.println("----------------------------------------------------------------------------------------------");

        for (ActivityLog log : logs) {

            String userId = log.getUserId() == null
                    ? "SYSTEM"
                    : String.valueOf(log.getUserId());

            System.out.printf(
                    "%-5d | %-8s | %-18s | %-35s | %-20s%n",
                    log.getLogId(),
                    userId,
                    shorten(log.getEventType(), 18),
                    shorten(log.getEventDescription(), 35),
                    shorten(log.getEventTimestamp(), 20)
            );
        }

        System.out.println("----------------------------------------------------------------------------------------------");
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
                System.out.println("Invalid input. Please enter a number.");
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