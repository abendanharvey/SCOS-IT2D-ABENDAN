
package com.schoolcanteen.ui;

import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.model.User;

import java.sql.SQLException;
import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n====================================");
            System.out.println("   SCHOOL CANTEEN ORDERING SYSTEM");
            System.out.println("====================================");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "0" -> {
                    System.out.println("Thank you for using the system!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void register() {
        System.out.println("\n=== REGISTER ACCOUNT ===");

        System.out.print("Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.println("\nSelect Role:");
        System.out.println("1. Student");
        System.out.println("2. Canteen Staff");
        System.out.println("3. Admin");
        System.out.print("Choose role: ");

        String roleChoice = scanner.nextLine().trim();
        String role;

        switch (roleChoice) {
            case "1" -> role = "STUDENT";
            case "2" -> role = "CANTEEN_STAFF";
            case "3" -> role = "ADMIN";
            default -> {
                System.out.println("Invalid role. Registration cancelled.");
                return;
            }
        }

        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || email.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }

        try {
            boolean registered = userDao.registerUser(
                    fullName, username, password, role, email);

            if (registered) {
                System.out.println(
                        "Registration successful! Your account is PENDING approval.");
            } else {
                System.out.println("Registration failed.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Registration failed. The username may already exist.");
        }
    }

    private void login() {
        System.out.println("\n=== LOGIN ===");

        System.out.print("Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            User user = userDao.findByUsername(username);

            if (user == null || !user.password().equals(password)) {
                System.out.println("Invalid username or password.");
                return;
            }

            if (!"APPROVED".equalsIgnoreCase(user.accountStatus())) {
                System.out.println(
                        "Your account is PENDING approval. Please contact an administrator.");
                return;
            }

            System.out.println("Login successful! Welcome, "
                    + user.fullName() + ".");

            routeUser(user);

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void routeUser(User user) {
        switch (user.role()) {
            case "STUDENT" ->
                    System.out.println("Opening Student Menu...");
            case "CANTEEN_STAFF" ->
                    System.out.println("Opening Canteen Staff Menu...");
            case "ADMIN" -> {
                System.out.println("Opening Admin Menu...");
                AdminMenu adminMenu = new AdminMenu(userDao, scanner);
                adminMenu.start();
            }
            default ->
                    System.out.println("Unknown user role.");
        }
    }
}
