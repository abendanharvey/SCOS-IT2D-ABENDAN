
package com.schoolcanteen.dao;

import com.schoolcanteen.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    // Register a new user account.
    public boolean registerUser(
            String fullName,
            String username,
            String password,
            String role,
            String email
    ) throws SQLException {

        String sql = """
                INSERT INTO Users
                (full_name, username, password, role, email, account_status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, role);
            statement.setString(5, email);

            return statement.executeUpdate() == 1;
        }
    }

    // Find a user by username.
    public User findByUsername(String username) throws SQLException {

        String sql = """
                SELECT user_id, full_name, username, password,
                       role, email, account_status
                FROM Users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {
                    return new User(
                            result.getInt("user_id"),
                            result.getString("full_name"),
                            result.getString("username"),
                            result.getString("password"),
                            result.getString("role"),
                            result.getString("email"),
                            result.getString("account_status")
                    );
                }
            }
        }

        return null;
    }

    // Get all registered users, regardless of role or status.
    public List<User> getAllUsers() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = """
                SELECT user_id, full_name, username, password,
                       role, email, account_status
                FROM Users
                ORDER BY user_id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(new User(
                        result.getInt("user_id"),
                        result.getString("full_name"),
                        result.getString("username"),
                        result.getString("password"),
                        result.getString("role"),
                        result.getString("email"),
                        result.getString("account_status")
                ));
            }
        }

        return users;
    }

    // Get users whose accounts are awaiting approval.
    public List<User> getPendingUsers() throws SQLException {

        List<User> users = new ArrayList<>();

        String sql = """
                SELECT user_id, full_name, username, password,
                       role, email, account_status
                FROM Users
                WHERE account_status = ?
                ORDER BY user_id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "PENDING");

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    users.add(new User(
                            result.getInt("user_id"),
                            result.getString("full_name"),
                            result.getString("username"),
                            result.getString("password"),
                            result.getString("role"),
                            result.getString("email"),
                            result.getString("account_status")
                    ));
                }
            }
        }

        return users;
    }

    // Approve a pending account using its user ID.
    public boolean approveUser(int userId) throws SQLException {

        String sql = """
                UPDATE Users
                SET account_status = ?
                WHERE user_id = ?
                  AND account_status = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "APPROVED");
            statement.setInt(2, userId);
            statement.setString(3, "PENDING");

            return statement.executeUpdate() == 1;
        }
    }

    // Update a user's role and account status.
    public boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus
    ) throws SQLException {

        String sql = """
                UPDATE Users
                SET role = ?, account_status = ?
                WHERE user_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newRole.toUpperCase());
            statement.setString(2, newStatus.toUpperCase());
            statement.setInt(3, userId);

            return statement.executeUpdate() == 1;
        }
    }
}
