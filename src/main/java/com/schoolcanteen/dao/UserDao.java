
package com.schoolcanteen.dao;

import com.schoolcanteen.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public boolean registerUser(
            String fullName,
            String username,
            String password,
            String role,
            String email) throws SQLException {

        String sql = """
                INSERT INTO Users
                (full_name, username, password, role, email, account_status)
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, fullName);
            statement.setString(2, username);
            statement.setString(3, password);
            statement.setString(4, role);
            statement.setString(5, email);

            return statement.executeUpdate() == 1;
        }
    }

    public User findByUsername(String username)
            throws SQLException {

        String sql = """
                SELECT user_id, full_name, username, password,
                       role, email, account_status
                FROM Users
                WHERE username = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

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
}
