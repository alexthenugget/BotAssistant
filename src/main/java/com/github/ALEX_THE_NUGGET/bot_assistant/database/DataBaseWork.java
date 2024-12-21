package com.github.ALEX_THE_NUGGET.bot_assistant.database;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.sql.*;

public class DataBaseWork {
    private final String dbPath = System.getenv("DB_PATH");

    public void connectTable(SendMessage message) {
        try (Connection conn = DriverManager.getConnection(dbPath);
             Statement stmt = conn.createStatement()) {

            String createTableSQL = """
                        CREATE TABLE IF NOT EXISTS users (
                            User_Id TEXT NOT NULL,
                            Date TEXT NOT NULL,
                            Time TEXT NOT NULL,
                            Task_Name TEXT NOT NULL
                        );
                    """;
            stmt.execute(createTableSQL);
            message.setText("База данных успешно подключена");
        } catch (SQLException e) {
            message.setText("Ошибка подключения к базе данных: " + e.getMessage());
        }
    }

    public void insertData(long chatId, String date, String time, String taskName, SendMessage message) {
        try (Connection conn = DriverManager.getConnection(dbPath)) {
            String insertSQL = "INSERT INTO users (User_Id, Date, Time, Task_Name) VALUES (?, ?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(insertSQL);
            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, date);
            preparedStatement.setString(3, time);
            preparedStatement.setString(4, taskName);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            message.setText("Ваше задание успешно добавлено.");
        } catch (SQLException e) {
            message.setText("Ошибка добавления данных: " + e.getMessage());
        }
    }

    public String getDataByDate(long chatId, String todayDate, SendMessage message) {
        String selectSQL = "SELECT Task_Name, Time FROM users WHERE User_Id = ? AND Date = ?";

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(selectSQL)) {

            stmt.setString(1, String.valueOf(chatId));
            stmt.setString(2, todayDate);

            try (ResultSet rs = stmt.executeQuery()) {
                StringBuilder result = new StringBuilder();
                while (rs.next()) {
                    result.append(rs.getString("Task_Name")).append(" | ")
                            .append(rs.getString("Time")).append('\n');
                }

                if (result.isEmpty()) {
                    return "У вас нет задач на сегодня " + todayDate + ".";
                }

                return "Ваши задачи на сегодня " + todayDate + ":\n" + "Задача | Время\n" + result.toString().trim();
            }
        } catch (SQLException e) {
            return "Ошибка получения данных: " + e.getMessage();
        }
    }
}
