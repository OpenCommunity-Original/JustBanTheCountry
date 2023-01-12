package org.opencommunity.JustBanTheCountry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

public class Main extends JavaPlugin {
    // SQLite connection
    private Connection connection;

    @Override
    public void onEnable() {
        // Get the plugin's data folder
        File dataFolder = getDataFolder();
        // Create the data folder if it does not exist
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        // Construct the path to the config file
        String configPath = dataFolder.getAbsolutePath() + File.separator + "config.yml";
        // Check if the config file exists
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            // If the config file does not exist, copy it from the resources folder
            try (InputStream in = getResource("config.yml")) {
                assert in != null;
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                Bukkit.getLogger().severe("Error copying config.yml from resources: " + e.getMessage());
                return;
            }
        }
        // Construct the path to the database file
        String dbPath = dataFolder.getAbsolutePath() + File.separator + "database.db";
        // Create the database file if it does not exist
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            try {
                dbFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Error creating SQLite database file: " + e.getMessage());
                return;
            }
        }
        // Connect to the SQLite database
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            Bukkit.getLogger().info("Successfully connected to SQLite database.");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error connecting to SQLite database: " + e.getMessage());
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS blacklist (name TEXT NOT NULL PRIMARY KEY)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS whitelist (name TEXT NOT NULL PRIMARY KEY)");
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error creating tables: " + e.getMessage());
            return;
        }

        // Load config
        FileConfiguration config = getConfig();

        // Register commands
        Objects.requireNonNull(getCommand("cwhitelist")).setExecutor(this);
        Objects.requireNonNull(getCommand("cwhitelist")).setTabCompleter(this);

        // Run
        Runner runner = new Runner(connection);
        runner.loadBlacklist(config);
        runner.loadKickMessage(config);
        Bukkit.getPluginManager().registerEvents(runner, this);
    }

    @Override
    public void onDisable() {
        saveConfig();

        // Close the SQLite connection
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            // No arguments, show the help message
            sender.sendMessage("Usage: /cwhitelist <add|remove> <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("justbanthecountry.usage")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Usage: /cwhitelist add <player>");
                return true;
            }
            // Add the player
            try {
                Runner.addPlayer(args[1]);
                sender.sendMessage("Player added.");
            } catch (SQLException e) {
                sender.sendMessage("Error adding player to : " + e.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("justbanthecountry.usage")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Usage: /cwhitelist add <player>");
                return true;
            }
            // Remove the player
            try {
                Runner.removePlayer(args[1]);
                sender.sendMessage("Player removed.");
            } catch (SQLException e) {
                sender.sendMessage("Error removing player from : " + e.getMessage());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("justbanthecountry.usage")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }
            // List of players
            // Get the list of whitelisted players from the database
            List<String> whitelist = getWhitelist();
            if (whitelist.isEmpty()) {
                sender.sendMessage("The whitelist is empty.");
            } else {
                sender.sendMessage("Whitelisted players: " + String.join(", ", whitelist));
            }
            return true;
        }

        // Invalid argument
        sender.sendMessage("Usage: /cwhitelist <add|remove> <player>");
        return true;
    }

    public List<String> getWhitelist() {
        List<String> whitelist = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT name FROM whitelist");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                whitelist.add(resultSet.getString("name"));
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error getting whitelist: " + e.getMessage());
        }
        return whitelist;
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            // Tab completing the subcommand
            return Arrays.asList("add", "remove", "list");
        }
        if (args.length == 2) {
            // Tab completing the player name
            List<String> onlinePlayers = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                onlinePlayers.add(player.getName());
            }
            return onlinePlayers;
        }
        // No tab completion
        return Collections.emptyList();
    }

}
