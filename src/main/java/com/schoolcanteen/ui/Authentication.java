
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
            System.out.println("\n=== SCHOOL CANTEEN ORDERING SYSTEM ===");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "3" -> {
                    running = false;
                    System.out.println("Thank you for using the system!");
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void register() {
        try {
            System.out.println("\n=== REGISTER ACCOUNT ===");

            System.out.print("Full name: ");
            String fullName = scanner.nextLine().trim();

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine();

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            System.out.println("Select role:");
            System.out.println("1. Student");
            System.out.println("2. Canteen Staff");
            System.out.println("3. Admin");
            System.out.print("Choose role: ");

            String roleChoice = scanner.nextLine().trim();

            String role = switch (roleChoice) {
                case "1" -> "STUDENT";
                case "2" -> "CANTEEN_STAFF";
                case "3" -> "ADMIN";
                default -> null;
            };

            if (fullName.isBlank() || username.isBlank()
                    || password.isBlank()) {
                System.out.println("Name, username, and password are required.");
                return;
            }

            if (role == null) {
                System.out.println("Invalid role selection.");
                return;
            }

            boolean registered = userDao.registerUser(
                    fullName, username, password, role, email);

            if (registered) {
                System.out.println(
                        "Registration successful! Your account is PENDING approval.");
            }

        } catch (SQLException e) {
            System.out.println(
                    "Registration failed. The username may already exist.");
        }
    }

    private void login() {
        try {
            System.out.println("\n=== LOGIN ===");

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine();

            User user = userDao.findByUsername(username);

            if (user == null || !user.password().equals(password)) {
                System.out.println("Invalid username or password.");
                return;
            }

            if (!"APPROVED".equalsIgnoreCase(user.accountStatus())) {
                System.out.println(
                        "Your account is PENDING approval. Please contact the administrator.");
                return;
            }

            System.out.println("Welcome, " + user.fullName() + "!");
            routeUser(user);

        } catch (SQLException e) {
            System.out.println("Login failed because of a database error.");
        }
    }

    private void routeUser(User user) {
        switch (user.role()) {
            case "STUDENT" ->
                    System.out.println("Opening Student Menu...");
            case "CANTEEN_STAFF" ->
                    System.out.println("Opening Canteen Staff Menu...");
            case "ADMIN" ->
                    System.out.println("Opening Admin Menu...");
            default ->
                    System.out.println("Unknown user role.");
        }
    }
}
