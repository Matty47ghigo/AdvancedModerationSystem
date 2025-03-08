package com.example.advancedModerationSystem;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    public static void createTable(Connection connection, String tableName, String columns) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" + columns + ")");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}