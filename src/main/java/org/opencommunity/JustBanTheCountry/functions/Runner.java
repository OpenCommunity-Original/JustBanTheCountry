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

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class Runner implements Listener {
    private final Set<String> blacklistedCountries;
    private static SQLiteAPI sqliteAPI;
    private static final Logger logger = Logger.getLogger("JustBanTheCountry");

    private final Component kickMessage;

    public Runner(Configuration config, SQLiteAPI sqliteAPI) {
        Runner.sqliteAPI = sqliteAPI;
        this.kickMessage = GsonComponentSerializer.gson().deserialize(Objects.requireNonNull(config.getString("kick-message")));
        this.blacklistedCountries = new HashSet<>(config.getStringList("blacklisted-countries"));
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        @NotNull String playerName = event.getName();
        @NotNull String playerUUID = String.valueOf(event.getUniqueId());

        try {
            // Check if the player is on the whitelist
            if (isPlayerWhitelisted(playerName)) {
                // If the player is on the whitelist, allow the login
                return;
            }
            // Look up the player's country using the EssentialsX geoip module
            String country = getPlayerCountry(playerName);
            // Check if the player is on the blacklist for trying to login from a blacklisted country
            if (isPlayerBlacklisted(playerUUID)) {
                // If the player is on the blacklist, kick the player with a message
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
            // Check if the player's country is blacklisted
            else if (country != null && blacklistedCountries.contains(country)) {
                // If the player is not on the blacklist, add them to the blacklist and kick them with a message
                addPlayerToBlacklist(playerUUID);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMessage);
            }
        } catch (SQLException | ExecutionException | InterruptedException e) {
            logger.warning("Error checking whitelist and blacklist for player " + playerName + ": " + e.getMessage());
        }
    }

    private String getPlayerCountry(@NotNull String player) {
        // Look up the player's country using the EssentialsX geoip module
        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        assert essentials != null;
        User user = essentials.getUser(player);

        String fullString = user.getGeoLocation();
        if (fullString != null) {
            String[] parts = fullString.split(",");
            return parts[parts.length - 1].trim();
        } else {
            logger.warning("Failed to get the region. Either the login is local or EssentialsX GeoIP is not configured.");
            return null;
        }
    }

    private void addPlayerToBlacklist(String name) throws SQLException {
        sqliteAPI.executeUpdate("INSERT INTO blacklist (blacklist_name) VALUES (?)", name).join();
    }

    public static void addPlayer(String name) throws SQLException {
        sqliteAPI.executeUpdate("INSERT INTO whitelist (whitelist_name) VALUES (?)", name).join();
    }

    public static void removePlayer(String name) throws SQLException {
        sqliteAPI.executeUpdate("DELETE FROM whitelist WHERE whitelist_name = ?", name).join();
    }

    public static boolean isPlayerWhitelisted(String name) throws SQLException, ExecutionException, InterruptedException {
        String query = "SELECT COUNT(*) FROM whitelist WHERE whitelist_name = ?";
        CompletableFuture<Boolean> future = sqliteAPI.hasResult(query, name);
        return future.get();
    }

    public static boolean isPlayerBlacklisted(String name) throws SQLException, ExecutionException, InterruptedException {
        String query = "SELECT COUNT(*) FROM blacklist WHERE blacklist_name = ?";

        CompletableFuture<Boolean> future = sqliteAPI.hasResult(query, name);
        return future.get();
    }
}