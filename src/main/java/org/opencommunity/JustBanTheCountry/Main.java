package org.opencommunity.JustBanTheCountry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.java.JavaPlugin;
import org.opencommunity.JustBanTheCountry.commands.WhitelistCommand;
import org.opencommunity.JustBanTheCountry.functions.Runner;
import org.opencommunity.JustBanTheCountry.utils.SQLiteAPI;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Main extends JavaPlugin {
    private Connection connection;
    private Configuration config;
    private WhitelistCommand whitelistCommand;
    private final SQLiteAPI api = new SQLiteAPI(getDataFolder().getAbsolutePath(), "database.db");

    @Override
    public void onEnable() {
        // Save the default config file if it doesn't already exist and reload the latest version
        saveAndReloadConfig();
        // Initialize and register the event listeners
        initializeEventListeners();
        // Initialize the database
        loadDatabase();
        // Initialize the chat command and register it with Bukkit
        initializeChatCommand();
    }

    @Override
    public void onDisable() {
        api.close();
    }

    private void saveAndReloadConfig() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
    }

    private void initializeEventListeners() {
        whitelistCommand = new WhitelistCommand(config, api);
        Runner runner = new Runner(config, api);
        Bukkit.getPluginManager().registerEvents(runner, this);
    }

    private void initializeChatCommand() {
        Objects.requireNonNull(getCommand("cwhitelist")).setExecutor(whitelistCommand);
        Objects.requireNonNull(getCommand("cwhitelist")).setTabCompleter(whitelistCommand);
    }

    private CompletableFuture<Boolean> loadDatabase() {
        return api.connect().thenCompose(aVoid ->
                api.executeUpdate("CREATE TABLE IF NOT EXISTS blacklist (name TEXT NOT NULL PRIMARY KEY)")
                        .thenCompose(blacklistResult ->
                                api.executeUpdate("CREATE TABLE IF NOT EXISTS whitelist (name TEXT NOT NULL PRIMARY KEY)")
                                        .thenApply(whitelistResult -> blacklistResult > 0 && whitelistResult > 0)));
    }
}
