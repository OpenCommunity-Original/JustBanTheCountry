package org.opencommunity.JustBanTheCountry;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Runner implements Listener {
    // Set up a Set to store the blacklist of countries
    private final Set<String> blacklistedCountries = new HashSet<>();

    // Set up a field to store the connection to the SQLite database
    private static Connection connection;

    public Component kickMessage;

    public Runner(Connection connection) {
        Runner.connection = connection;
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

        return parts[parts.length-1].trim();
    }

    private boolean isPlayerBlacklisted(String name) throws SQLException {
        // Check if the player is on the blacklist for trying to login from a blacklisted country
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM blacklist WHERE name = ?");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.getInt(1) > 0;
    }

    private void addPlayerToBlacklist(String name) throws SQLException {
        // If the player is not on the blacklist, add them to the blacklist
        PreparedStatement statement = connection.prepareStatement("INSERT INTO blacklist (name) VALUES (?)");
        statement.setString(1, name);
        statement.executeUpdate();
    }

    public static void addPlayer(String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("INSERT INTO whitelist (name) VALUES (?)");
        statement.setString(1, name);
        statement.executeUpdate();
    }

    public static void removePlayer(String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("DELETE FROM whitelist WHERE name = ?");
        statement.setString(1, name);
        statement.executeUpdate();
    }

    public boolean isPlayerWhitelisted(String name) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM whitelist WHERE name = ?");
        statement.setString(1, name);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.getInt(1) > 0;
    }

    public void loadBlacklist(FileConfiguration config) {
        blacklistedCountries.clear();
        List<String> countries = config.getStringList("blacklisted-countries");
        blacklistedCountries.addAll(countries);
    }

    public void loadKickMessage(FileConfiguration config) {
        // Load kick message
        String rawKickMessage = config.getString("Kick-message");
        assert rawKickMessage != null;
        kickMessage = GsonComponentSerializer.gson().deserialize(rawKickMessage);
    }
}