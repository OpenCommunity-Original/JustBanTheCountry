package org.opencommunity.JustBanTheCountry.functions;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.opencommunity.JustBanTheCountry.utils.SQLiteAPI;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Runner implements Listener {
    private Set<String> blacklistedCountries = new HashSet<>();
    private final Configuration config;
    private static SQLiteAPI sqliteAPI;

    private Component kickMessage;

    public Runner(Configuration config, SQLiteAPI sqliteAPI) {
        this.config = config;
        Runner.sqliteAPI = sqliteAPI;
        this.kickMessage = GsonComponentSerializer.gson().deserialize(Objects.requireNonNull(config.getString("kick-message")));
        this.blacklistedCountries = new HashSet<>(config.getStringList("blacklisted-countries"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        @NotNull String playerName = event.getName();
        try {
            // Check if the player is on the whitelist
            if (isPlayerWhitelisted(playerName)) {
                // If the player is on the whitelist, allow the login
                return;
            }
            // Look up the player's country using the EssentialsX geoip module
            String country = getPlayerCountry(playerName);
            // Check if the player's country is blacklisted
            if (blacklistedCountries.contains(country)) {
                // Check if the player is on the blacklist for trying to login from a blacklisted country
                if (isPlayerBlacklisted(playerName)) {
                    // If the player is on the blacklist, kick the player with a message
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
                } else {
                    // If the player is not on the blacklist, add them to the blacklist and kick them with a message
                    addPlayerToBlacklist(playerName);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().warning("Error checking whitelist and blacklist for player " + playerName + ": " + e.getMessage());
        }
    }

    private String getPlayerCountry(@NotNull String player) {
        // Look up the player's country using the EssentialsX geoip module
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        assert essentials != null;
        User user = essentials.getUser(player);

        String fullString = user.getGeoLocation();
        String[] parts = fullString.split(",");

        return parts[parts.length - 1].trim();
    }

    private void addPlayerToBlacklist(String name) throws SQLException {
        sqliteAPI.executeUpdate("INSERT INTO blacklist (name) VALUES (?)", name).join();
    }

    public static void addPlayer(String name) throws SQLException {
        sqliteAPI.executeUpdate("INSERT INTO whitelist (name) VALUES (?)", name).join();
    }

    public static void removePlayer(String name) throws SQLException {
        sqliteAPI.executeUpdate("DELETE FROM whitelist WHERE name = ?", name).join();
    }

    public boolean isPlayerWhitelisted(String name) throws SQLException {
        ResultSet resultSet = sqliteAPI.executeQuery("SELECT COUNT(*) FROM whitelist WHERE name = ?", name).join();;
        boolean isWhitelisted = false;
        if (resultSet.next()) {
            isWhitelisted = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return isWhitelisted;
    }

    public boolean isPlayerBlacklisted(String name) throws SQLException {
        ResultSet resultSet = sqliteAPI.executeQuery("SELECT COUNT(*) FROM blacklist WHERE name = ?", name).join();;
        boolean isBlacklisted = false;
        if (resultSet.next()) {
            isBlacklisted = resultSet.getInt(1) > 0;
        }
        resultSet.close();
        return isBlacklisted;
    }
}