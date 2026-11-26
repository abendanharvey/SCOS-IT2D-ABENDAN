package com.schoolcanteen.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class OrderDao {

    private final ActivityLogDao activityLogDao;

    public OrderDao() {
        this.activityLogDao = new ActivityLogDao();
    }

    // PLACE ORDER
    public int placeOrder(int userId, int foodId, int quantity)
            throws SQLException {

        if (userId <= 0 || foodId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException(
                    "User ID, Food ID, and quantity must be valid.");
        }

        String foodSql = """
                SELECT price, availability
                FROM Food_Items
                WHERE food_id = ?
                """;

        String orderSql = """
                INSERT INTO Orders (user_id, total_amount, order_status)
                VALUES (?, ?, 'PENDING')
                """;

        String detailSql = """
                INSERT INTO Order_Details
                (order_id, food_id, quantity, subtotal)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                double price;

                try (PreparedStatement foodStatement =
                             connection.prepareStatement(foodSql)) {

                    foodStatement.setInt(1, foodId);

                    try (ResultSet result = foodStatement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException("Food item not found.");
                        }

                        if (!"AVAILABLE".equalsIgnoreCase(
                                result.getString("availability"))) {
                            throw new SQLException(
                                    "Food item is not available.");
                        }

                        price = result.getDouble("price");
                    }
                }

                double subtotal = price * quantity;
                int orderId;

                try (PreparedStatement orderStatement =
                             connection.prepareStatement(
                                     orderSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    orderStatement.setInt(1, userId);
                    orderStatement.setDouble(2, subtotal);
                    orderStatement.executeUpdate();

                    try (ResultSet keys =
                                 orderStatement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to retrieve order ID.");
                        }

                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement detailStatement =
                             connection.prepareStatement(detailSql)) {

                    detailStatement.setInt(1, orderId);
                    detailStatement.setInt(2, foodId);
                    detailStatement.setInt(3, quantity);
                    detailStatement.setDouble(4, subtotal);

                    detailStatement.executeUpdate();
                }

                connection.commit();

                activityLogDao.logAction(
                        userId,
                        "ORDER_PLACED",
                        "Student [" + userId
                                + "] placed a food order. Order ID: ["
                                + orderId + "]."
                );

                return orderId;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // VIEW PENDING ORDERS (STAFF)
    public void viewPendingOrders() throws SQLException {

        String sql = """
                SELECT
                    Orders.order_id,
                    Users.full_name,
                    Food_Items.food_name,
                    Order_Details.quantity,
                    Order_Details.subtotal,
                    Orders.total_amount,
                    Orders.order_date
                FROM Orders
                JOIN Users ON Orders.user_id = Users.user_id
                JOIN Order_Details
                    ON Orders.order_id = Order_Details.order_id
                JOIN Food_Items
                    ON Order_Details.food_id = Food_Items.food_id
                WHERE Orders.order_status = 'PENDING'
                ORDER BY Orders.order_id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\n========== PENDING ORDERS ==========");

            boolean found = false;

            while (result.next()) {
                found = true;

                System.out.println("------------------------------------");
                System.out.println("Order ID: " + result.getInt("order_id"));
                System.out.println("Student: " + result.getString("full_name"));
                System.out.println("Food: " + result.getString("food_name"));
                System.out.println("Quantity: " + result.getInt("quantity"));
                System.out.println("Subtotal: PHP " + result.getDouble("subtotal"));
                System.out.println("Order Total: PHP " + result.getDouble("total_amount"));
                System.out.println("Order Date: " + result.getString("order_date"));
                System.out.println("Status: PENDING");
            }

            if (!found) {
                System.out.println("No pending orders found.");
            }

            System.out.println("------------------------------------");
        }
    }

    // APPROVE ORDER AND DEDUCT STOCK
    public boolean updateOrderStatus(
            int updaterUserId,
            int orderId,
            String newStatus) throws SQLException {

        if (orderId <= 0 || newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid order information.");
        }

        newStatus = newStatus.trim().toUpperCase();

        if (!newStatus.equals("APPROVED")) {
            throw new IllegalArgumentException(
                    "Only APPROVED status is supported here.");
        }

        String orderCheckSql = """
                SELECT order_status
                FROM Orders
                WHERE order_id = ?
                """;

        String detailSql = """
                SELECT food_id, quantity
                FROM Order_Details
                WHERE order_id = ?
                """;

        String stockSql = """
                UPDATE Food_Items
                SET stock_quantity = stock_quantity - ?,
                    availability = CASE
                        WHEN stock_quantity - ? <= 0 THEN 'UNAVAILABLE'
                        ELSE 'AVAILABLE'
                    END
                WHERE food_id = ?
                AND stock_quantity >= ?
                """;

        String updateOrderSql = """
                UPDATE Orders
                SET order_status = 'APPROVED'
                WHERE order_id = ?
                AND order_status = 'PENDING'
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // CHECK ORDER STATUS
                try (PreparedStatement checkStatement =
                             connection.prepareStatement(orderCheckSql)) {

                    checkStatement.setInt(1, orderId);

                    try (ResultSet result = checkStatement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException("Order not found.");
                        }

                        if (!"PENDING".equalsIgnoreCase(
                                result.getString("order_status"))) {
                            throw new SQLException(
                                    "This order is not pending anymore.");
                        }
                    }
                }

                // GET ORDER DETAILS
                int foodId;
                int quantity;

                try (PreparedStatement detailStatement =
                             connection.prepareStatement(detailSql)) {

                    detailStatement.setInt(1, orderId);

                    try (ResultSet result = detailStatement.executeQuery()) {

                        if (!result.next()) {
                            throw new SQLException(
                                    "Order details not found.");
                        }

                        foodId = result.getInt("food_id");
                        quantity = result.getInt("quantity");
                    }
                }

                // DEDUCT STOCK
                try (PreparedStatement stockStatement =
                             connection.prepareStatement(stockSql)) {

                    stockStatement.setInt(1, quantity);
                    stockStatement.setInt(2, quantity);
                    stockStatement.setInt(3, foodId);
                    stockStatement.setInt(4, quantity);

                    int stockUpdated = stockStatement.executeUpdate();

                    if (stockUpdated != 1) {
                        throw new SQLException(
                                "Insufficient food stock to approve this order.");
                    }
                }

                // UPDATE ORDER STATUS
                try (PreparedStatement updateStatement =
                             connection.prepareStatement(updateOrderSql)) {

                    updateStatement.setInt(1, orderId);

                    if (updateStatement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Failed to approve order.");
                    }
                }

                connection.commit();

                activityLogDao.logAction(
                        updaterUserId,
                        "ORDER_STATUS_UPDATED",
                        "Admin/Staff [" + updaterUserId
                                + "] approved Order #" + orderId
                                + " and deducted food stock."
                );

                System.out.println(
                        "Order approved and stock deducted successfully.");

                return true;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // VIEW FOOD STOCKS
    public void viewFoodStocks() throws SQLException {

        String sql = """
                SELECT
                    food_id,
                    food_name,
                    price,
                    stock_quantity,
                    availability
                FROM Food_Items
                ORDER BY food_id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\n================ FOOD STOCKS ================");
            System.out.printf("%-5s %-22s %-12s %-10s %-15s%n",
                    "ID", "Food Name", "Price", "Stock", "Availability");

            System.out.println(
                    "------------------------------------------------------------");

            boolean found = false;

            while (result.next()) {
                found = true;

                System.out.printf(
                        "%-5d %-22s PHP %-8.2f %-10d %-15s%n",
                        result.getInt("food_id"),
                        result.getString("food_name"),
                        result.getDouble("price"),
                        result.getInt("stock_quantity"),
                        result.getString("availability")
                );
            }

            if (!found) {
                System.out.println("No food items found.");
            }

            System.out.println(
                    "------------------------------------------------------------");
        }
    }

    // RESTOCK FOOD
    public boolean restockFood(
            int foodId,
            int quantity,
            int staffUserId) throws SQLException {

        if (foodId <= 0 || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Food ID and quantity must be positive.");
        }

        String sql = """
                UPDATE Food_Items
                SET stock_quantity = stock_quantity + ?,
                    availability = 'AVAILABLE'
                WHERE food_id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, quantity);
            statement.setInt(2, foodId);

            int rowsUpdated = statement.executeUpdate();

            if (rowsUpdated == 1) {

                activityLogDao.logAction(
                        staffUserId,
                        "FOOD_RESTOCKED",
                        "Staff [" + staffUserId
                                + "] restocked Food ID [" + foodId
                                + "] by " + quantity + " units."
                );

                return true;
            }

            return false;
        }
    }

    // VIEW MY PENDING ORDERS (STUDENT)
    public void viewMyOrders(int userId) throws SQLException {

        String sql = """
                SELECT
                    Orders.order_id,
                    Food_Items.food_name,
                    Order_Details.quantity,
                    Order_Details.subtotal,
                    Orders.total_amount,
                    Orders.order_status,
                    Orders.order_date
                FROM Orders
                JOIN Order_Details
                    ON Orders.order_id = Order_Details.order_id
                JOIN Food_Items
                    ON Order_Details.food_id = Food_Items.food_id
                WHERE Orders.user_id = ?
                AND Orders.order_status = 'PENDING'
                ORDER BY Orders.order_id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                System.out.println("\n========== MY PENDING ORDERS ==========");

                boolean found = false;

                while (result.next()) {
                    found = true;

                    System.out.println("------------------------------------");
                    System.out.println("Order ID: " + result.getInt("order_id"));
                    System.out.println("Food: " + result.getString("food_name"));
                    System.out.println("Quantity: " + result.getInt("quantity"));
                    System.out.println("Subtotal: PHP " + result.getDouble("subtotal"));
                    System.out.println("Order Total: PHP " + result.getDouble("total_amount"));
                    System.out.println("Status: " + result.getString("order_status"));
                    System.out.println("Order Date: " + result.getString("order_date"));
                }

                if (!found) {
                    System.out.println("You have no pending orders.");
                }

                System.out.println("------------------------------------");
            }
        }
    }

    // VIEW APPROVED ORDER HISTORY (STUDENT)
    public void viewOrderHistory(int userId) throws SQLException {

        String sql = """
                SELECT
                    Orders.order_id,
                    Food_Items.food_name,
                    Order_Details.quantity,
                    Order_Details.subtotal,
                    Orders.total_amount,
                    Orders.order_status,
                    Orders.order_date
                FROM Orders
                JOIN Order_Details
                    ON Orders.order_id = Order_Details.order_id
                JOIN Food_Items
                    ON Order_Details.food_id = Food_Items.food_id
                WHERE Orders.user_id = ?
                AND Orders.order_status = 'APPROVED'
                ORDER BY Orders.order_id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {

                System.out.println("\n========== APPROVED ORDER HISTORY ==========");

                boolean found = false;

                while (result.next()) {
                    found = true;

                    System.out.println("------------------------------------");
                    System.out.println("Order ID: " + result.getInt("order_id"));
                    System.out.println("Food: " + result.getString("food_name"));
                    System.out.println("Quantity: " + result.getInt("quantity"));
                    System.out.println("Subtotal: PHP " + result.getDouble("subtotal"));
                    System.out.println("Order Total: PHP " + result.getDouble("total_amount"));
                    System.out.println("Status: " + result.getString("order_status"));
                    System.out.println("Order Date: " + result.getString("order_date"));
                }

                if (!found) {
                    System.out.println(
                            "You have no approved orders in your history.");
                }

                System.out.println("------------------------------------");
            }
        }
    }
}