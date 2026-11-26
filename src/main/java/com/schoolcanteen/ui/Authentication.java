package com.schoolcanteen.ui;

import com.schoolcanteen.dao.UserDao;
import com.schoolcanteen.dao.ActivityLogDao;
import com.schoolcanteen.model.User;

import java.sql.SQLException;
import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public Authentication(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.activityLogDao = new ActivityLogDao();
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n========== SCHOOL CANTEEN SYSTEM ==========");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("0. Exit");
            System.out.println("===========================================");

            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> login();
                case "0" -> {
                    System.out.println("Thank you for using the system.");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // REGISTER
    private void register() {
        System.out.println("\n========== USER REGISTRATION ==========");

        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine().trim();

        System.out.println("\nSelect Role:");
        System.out.println("1. STUDENT");
        System.out.println("2. CANTEEN_STAFF");
        System.out.println("3. ADMIN");

        System.out.print("Enter choice: ");
        String roleChoice = scanner.nextLine().trim();

        String role;

        switch (roleChoice) {
            case "1" -> role = "STUDENT";
            case "2" -> role = "CANTEEN_STAFF";
            case "3" -> role = "ADMIN";
            default -> {
                System.out.println("Invalid role selection.");
                return;
            }
        }

        if (fullName.isEmpty() || username.isEmpty()
                || password.isEmpty() || email.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }

        try {
            userDao.registerUser(fullName, username, password, role, email);

            System.out.println(
                    "Registration successful! Your account is PENDING approval."
            );

        } catch (SQLException e) {
            System.out.println("Registration failed. Username may already exist.");
            e.printStackTrace();
        }
    }

    // LOGIN
    private void login() {
        System.out.println("\n========== USER LOGIN ==========");

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = userDao.findByUsername(username);

            if (user == null || !user.password().equals(password)) {
                System.out.println("Invalid username or password.");
                return;
            }

            if (!user.accountStatus().equalsIgnoreCase("APPROVED")) {
                System.out.println("Your account is still pending approval.");
                return;
            }

            System.out.println("Login successful! Welcome, " + user.fullName() + ".");

            activityLogDao.logAction(
                    user.userId(),
                    "LOGIN",
                    "User " + user.username() + " logged in."
            );

            System.out.println("Login activity recorded successfully.");

            routeUser(user);

            activityLogDao.logAction(
                    user.userId(),
                    "LOGOUT",
                    "User " + user.username() + " logged out."
            );

            System.out.println("Logout activity recorded successfully.");

        } catch (SQLException e) {
            System.out.println("Login or activity logging failed due to a database error.");
            e.printStackTrace();
        }
    }

    // ROUTE USER
    private void routeUser(User user) {

        switch (user.role().toUpperCase()) {

            case "STUDENT" -> {
                StudentMenu studentMenu = new StudentMenu(scanner, user.userId());
                studentMenu.start();
            }

            case "CANTEEN_STAFF" -> {
                CanteenStaffMenu staffMenu = new CanteenStaffMenu(scanner, user.userId());
                staffMenu.start();
            }

            case "ADMIN" -> {
                System.out.println("\nOpening Admin Menu...");

                AdminMenu adminMenu = new AdminMenu(
                        userDao,
                        scanner,
                        user
                );

                adminMenu.start();
            }

            default -> System.out.println("Unknown user role.");
        }
    }
}