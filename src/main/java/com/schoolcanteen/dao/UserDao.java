
package com.schoolcanteen.dao;

import com.schoolcanteen.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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

    // Get all registered users.
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

    // Get users awaiting approval.
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

    // Approve an account.
    // This version keeps compatibility with existing calls.
    public boolean approveUser(int userId) throws SQLException {
        return approveUser(userId, null);
    }

    // Approve an account and record the administrator who performed it.
    public boolean approveUser(
            int targetUserId,
            Integer adminUserId
    ) throws SQLException {

        String sql = """
                UPDATE Users
                SET account_status = ?
                WHERE user_id = ?
                  AND account_status = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int updatedRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, "APPROVED");
                    statement.setInt(2, targetUserId);
                    statement.setString(3, "PENDING");

                    updatedRows = statement.executeUpdate();
                }

                if (updatedRows != 1) {
                    connection.rollback();
                    return false;
                }

                insertActivityLog(
                        connection,
                        adminUserId,
                        "USER_APPROVED",
                        "Administrator approved user ID: " + targetUserId
                );

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Update a user's role and account status.
    // This version keeps compatibility with existing calls.
    public boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus
    ) throws SQLException {

        return updateUserRoleAndStatus(
                userId, newRole, newStatus, null
        );
    }

    // Update a user and record the administrator who performed it.
    public boolean updateUserRoleAndStatus(
            int targetUserId,
            String newRole,
            String newStatus,
            Integer adminUserId
    ) throws SQLException {

        String sql = """
                UPDATE Users
                SET role = ?, account_status = ?
                WHERE user_id = ?
                """;

        String role = newRole.toUpperCase();
        String status = newStatus.toUpperCase();

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int updatedRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, role);
                    statement.setString(2, status);
                    statement.setInt(3, targetUserId);

                    updatedRows = statement.executeUpdate();
                }

                if (updatedRows != 1) {
                    connection.rollback();
                    return false;
                }

                insertActivityLog(
                        connection,
                        adminUserId,
                        "USER_UPDATED",
                        "Administrator updated user ID: "
                                + targetUserId
                                + " | New Role: " + role
                                + " | New Status: " + status
                );

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Insert an activity record using the same database transaction.
    private void insertActivityLog(
            Connection connection,
            Integer adminUserId,
            String eventType,
            String description
    ) throws SQLException {

        String sql = """
                INSERT INTO Activity_Logs
                (user_id, event_type, event_description)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            if (adminUserId == null || adminUserId <= 0) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, adminUserId);
            }

            statement.setString(2, eventType);
            statement.setString(3, description);

            statement.executeUpdate();
        }
    }
}
