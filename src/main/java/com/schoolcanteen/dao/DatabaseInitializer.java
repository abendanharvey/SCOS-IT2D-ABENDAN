
package com.schoolcanteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() {
        String[] tables = {

                """
            CREATE TABLE IF NOT EXISTS Users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                username TEXT NOT NULL UNIQUE,
                password TEXT NOT NULL,
                role TEXT NOT NULL CHECK (
                    role IN ('STUDENT', 'CANTEEN_STAFF', 'ADMIN')
                ),
                email TEXT,
                account_status TEXT NOT NULL DEFAULT 'PENDING'
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS Activity_Logs (
                log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                event_type TEXT NOT NULL,
                event_description TEXT,
                event_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES Users(user_id)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS Food_Items (
                food_id INTEGER PRIMARY KEY AUTOINCREMENT,
                food_name TEXT NOT NULL,
                category TEXT,
                price REAL NOT NULL CHECK (price >= 0),
                availability TEXT NOT NULL DEFAULT 'AVAILABLE'
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS Orders (
                order_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_amount REAL NOT NULL DEFAULT 0,
                order_status TEXT NOT NULL DEFAULT 'PENDING',
                FOREIGN KEY (user_id) REFERENCES Users(user_id)
            )
            """,

                """
            CREATE TABLE IF NOT EXISTS Order_Details (
                order_detail_id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id INTEGER NOT NULL,
                food_id INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                subtotal REAL NOT NULL CHECK (subtotal >= 0),
                FOREIGN KEY (order_id) REFERENCES Orders(order_id),
                FOREIGN KEY (food_id) REFERENCES Food_Items(food_id)
            )
            """
        };

        try (Connection connection =
                     DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            for (String sql : tables) {
                statement.execute(sql);
            }

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Database initialization failed.");
            e.printStackTrace();
            throw new IllegalStateException(
                    "Unable to initialize the database.", e);
        }
    }
}
