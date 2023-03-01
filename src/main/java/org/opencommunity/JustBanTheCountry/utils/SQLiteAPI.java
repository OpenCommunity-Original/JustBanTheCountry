package org.opencommunity.JustBanTheCountry.utils;

import java.io.File;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class SQLiteAPI {

    private final String databasePath;
    private Connection connection;

    public SQLiteAPI(String pluginFolder, String databaseName) {
        this.databasePath = pluginFolder + File.separator + databaseName;
    }

    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<ResultSet> executeQuery(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            ResultSet resultSet = null;
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setObject(i+1, parameters[i]);
                }
                resultSet = stmt.executeQuery();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return resultSet;
        });
    }

    public CompletableFuture<Integer> executeUpdate(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
                return statement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    public CompletableFuture<Boolean> hasResult(String query, String name) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    boolean match = resultSet.next() && resultSet.getInt(1) > 0;
                    future.complete(match);
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        thread.start();
        return future;
    }


}