
package com.schoolcanteen.dao;

import com.schoolcanteen.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // Record a new activity.
    public boolean logAction(
            int userId,
            String actionType,
            String description
    ) throws SQLException {

        String sql = """
                INSERT INTO Activity_Logs
                (user_id, event_type, event_description)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (userId <= 0) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }

            statement.setString(2, actionType);
            statement.setString(3, description);

            return statement.executeUpdate() > 0;
        }
    }

    // Retrieve all system activity logs.
    public List<ActivityLog> getAllLogs() throws SQLException {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT * FROM Activity_Logs
                ORDER BY event_timestamp DESC, log_id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                logs.add(mapLog(result));
            }
        }

        return logs;
    }

    // Retrieve logs belonging to a specific user.
    public List<ActivityLog> getLogsByUserId(int userId)
            throws SQLException {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT * FROM Activity_Logs
                WHERE user_id = ?
                ORDER BY event_timestamp DESC, log_id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    logs.add(mapLog(result));
                }
            }
        }

        return logs;
    }

    // Clear historical logs and preserve a record of the operation.
    public boolean clearAllLogs(int adminUserId) throws SQLException {

        String deleteSql = "DELETE FROM Activity_Logs";

        String logSql = """
                INSERT INTO Activity_Logs
                (user_id, event_type, event_description)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int deletedCount;

                try (PreparedStatement deleteStatement =
                             connection.prepareStatement(deleteSql)) {

                    deletedCount = deleteStatement.executeUpdate();
                }

                try (PreparedStatement logStatement =
                             connection.prepareStatement(logSql)) {

                    if (adminUserId <= 0) {
                        logStatement.setNull(1, Types.INTEGER);
                    } else {
                        logStatement.setInt(1, adminUserId);
                    }

                    logStatement.setString(2, "LOGS_CLEARED");
                    logStatement.setString(
                            3,
                            "Administrator cleared historical activity logs. "
                                    + "Records deleted: " + deletedCount
                    );

                    logStatement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Convert database records into ActivityLog objects.
    private ActivityLog mapLog(ResultSet result)
            throws SQLException {

        int id = result.getInt("user_id");

        Integer userId = result.wasNull() ? null : id;

        return new ActivityLog(
                result.getInt("log_id"),
                userId,
                result.getString("event_type"),
                result.getString("event_description"),
                result.getString("event_timestamp")
        );
    }
}
