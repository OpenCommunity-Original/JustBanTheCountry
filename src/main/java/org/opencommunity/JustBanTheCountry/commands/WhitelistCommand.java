package org.opencommunity.JustBanTheCountry.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.opencommunity.JustBanTheCountry.functions.Runner;
import org.opencommunity.JustBanTheCountry.utils.SQLiteAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WhitelistCommand implements CommandExecutor, TabCompleter {

    private final Configuration config;
    private static SQLiteAPI sqliteAPI;

    public WhitelistCommand(Configuration config, SQLiteAPI sqliteAPI) {
        this.config = config;
        this.sqliteAPI = sqliteAPI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, String[] args) {
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
            List<String> whitelist = null;
            try {
                whitelist = getWhitelist();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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

    public List<String> getWhitelist() throws SQLException {
        List<String> whitelist = new ArrayList<>();
        ResultSet resultSet = sqliteAPI.executeQuery("SELECT name FROM whitelist").join();
        while (resultSet.next()) {
            whitelist.add(resultSet.getString("name"));
        }
        return whitelist;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias, String[] args) {
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
