/**
 * This file is part of Coins
 *
 * Copyright © 2018 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.beelzebu.coins.api;

import io.github.beelzebu.coins.api.CoinsResponse.CoinsResponseType;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoinsAPI {

    private static final DecimalFormat DF = new DecimalFormat("#.#");
    private static CoinsPlugin PLUGIN = null;

    /**
     * Get the coins of a Player by his name.
     *
     * @param name Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull String name) {
        return PLUGIN.getCache().getCoins(PLUGIN.getUniqueId(name, false)).orElse(PLUGIN.getDatabase().getCoins(PLUGIN.getUniqueId(name, false)));
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull UUID uuid) {
        return PLUGIN.getCache().getCoins(uuid).orElse(PLUGIN.getDatabase().getCoins(uuid));
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param name Player to get the coins string.
     * @return Coins in decimal format "#.#"
     */
    public static String getCoinsString(@Nonnull String name) {
        double coins = getCoins(name.toLowerCase());
        if (coins >= 0) {
            return DF.format(coins);
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param uuid Player to get the coins string.
     * @return Coins in decimal format "#.#"
     */
    public static String getCoinsString(@Nonnull UUID uuid) {
        double coins = getCoins(uuid);
        if (coins >= 0) {
            return DF.format(coins);
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param name     Player to add the coins.
     * @param coins    Coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse addCoins(@Nonnull String name, double coins, boolean multiply) {
        return addCoins(PLUGIN.getUniqueId(name, false), coins, multiply);
    }

    /**
     * Add coins to a player by his UUID, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param uuid     Player to add the coins.
     * @param coins    Coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse addCoins(@Nonnull UUID uuid, double coins, boolean multiply) {
        if (!isindb(uuid)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
        double finalCoins = coins;
        if (multiply && !getMultipliers().isEmpty()) {
            int multiplyTotal = getMultipliers().stream().filter(multiplier -> multiplier.getType().equals(MultiplierType.PERSONAL) && uuid.equals(multiplier.getData().getEnablerUUID())).filter(Multiplier::isEnabled).mapToInt(multiplier -> multiplier.getData().getAmount()).sum();
            finalCoins *= multiplyTotal >= 1 ? multiplyTotal : 1;
            for (String perm : PLUGIN.getBootstrap().getPermissions(uuid)) {
                if (perm.startsWith("coins.multiplier.x")) {
                    try {
                        int i = Integer.parseInt(perm.split("coins.multiplier.x")[1]);
                        finalCoins *= i;
                        break;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        finalCoins += getCoins(uuid);
        return setCoins(uuid, finalCoins);
    }

    /**
     * Take coins of a player by his name.
     *
     * @param name  The name of the player to take the coins.
     * @param coins Coins to take from the player.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse takeCoins(@Nonnull String name, double coins) {
        return setCoins(PLUGIN.getUniqueId(name, false), getCoins(name) - coins);
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param uuid  The UUID of the player to take the coins.
     * @param coins Coins to take from the player.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse takeCoins(@Nonnull UUID uuid, double coins) {
        return setCoins(uuid, getCoins(uuid) - coins);
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param name The name of the player to reset the coins.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse resetCoins(@Nonnull String name) {
        return setCoins(PLUGIN.getUniqueId(name, false), PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Reset the coins of a player by his UUID.
     *
     * @param uuid The UUID of the player to reset the coins.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse resetCoins(@Nonnull UUID uuid) {
        return setCoins(uuid, PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param name  The name of the player to set the coins.
     * @param coins Coins to set.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse setCoins(@Nonnull String name, double coins) {
        return setCoins(PLUGIN.getUniqueId(name, false), coins);
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param uuid  The UUID of the player to set the coins.
     * @param coins Coins to set.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse setCoins(@Nonnull UUID uuid, double coins) {
        if (isindb(uuid)) {
            PLUGIN.getMessagingService().publishUser(uuid, coins);
            return PLUGIN.getDatabase().setCoins(uuid, coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse payCoins(@Nonnull String from, @Nonnull String to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
        }
        return new CoinsResponse(CoinsResponseType.FAILED, "The user from doesn't have enough coins.");
    }

    /**
     * Pay coins to another player.
     *
     * @param from   The player to get the coins.
     * @param to     The player to pay.
     * @param amount The amount of coins to pay.
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse payCoins(@Nonnull UUID from, @Nonnull UUID to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
        }
        return new CoinsResponse(CoinsResponseType.FAILED, "The user from doesn't have sufficient coins.");
    }

    /**
     * Get if a player with the specified name exists in the database. Is not
     * recommended check a player by his name because it can change.
     *
     * @param name The name to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(@Nonnull String name) {
        UUID uuid = PLUGIN.getUniqueId(name, false);
        if (getCoins(uuid != null ? uuid : UUID.randomUUID()) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return PLUGIN.getDatabase().isindb(name);
    }

    /**
     * Get if a player with the specified uuid exists in the database.
     *
     * @param uuid The uuid to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(@Nonnull UUID uuid) {
        if (getCoins(uuid) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return PLUGIN.getDatabase().isindb(uuid);
    }

    /**
     * Get the top players in coins data.
     *
     * @param top The length of the top list, for example 5 will get a max of 5
     *            users for the top.
     * @return The ordered top list of players and his balance.
     */
    public static LinkedHashMap<String, Double> getTopPlayers(int top) {
        return PLUGIN.getDatabase().getTopPlayers(top);
    }

    /**
     * Register a user in the database with the default starting balance.
     *
     * @param nick The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static void createPlayer(@Nonnull String nick, UUID uuid) {
        createPlayer(nick, uuid, PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Register a user in the database with the specified balance.
     *
     * @param nick    The name of the user that will be registered.
     * @param uuid    The uuid of the user.
     * @param balance The balance of the user.
     */
    public static void createPlayer(@Nonnull String nick, UUID uuid, double balance) {
        PLUGIN.getDatabase().createPlayer(uuid, nick, balance);
    }

    /**
     * Get all enabled multipliers for this server.
     *
     * @return The active multiplier for this server.
     */
    public static Set<Multiplier> getMultipliers() {
        return getMultipliers(PLUGIN.getConfig().getString("Multipliers.Server", "default"));
    }

    /**
     * Get all enabled multipliers in this server.
     *
     * @param server The server to modify and get info about multiplier.
     * @return The active multiplier for the specified server can be null;
     */
    public static Set<Multiplier> getMultipliers(@Nonnull String server) {
        return PLUGIN.getCache().getMultipliers(server);
    }

    /**
     * Get a multiplier from the database by his ID and add it to the cache.
     *
     * @param id The ID of the multiplier.
     * @return The multiplier from the Cache.
     */
    public static Multiplier getMultiplier(int id) {
        return PLUGIN.getCache().getMultiplier(id).orElse(PLUGIN.getDatabase().getMultiplier(id));
    }

    /**
     * Get all multipliers for a player from the database.
     *
     * @param uuid player to get the multipliers from the database.
     * @return all multipliers that this player have.
     */
    public static Set<Multiplier> getAllMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getDatabase().getMultipliers(uuid);
    }

    /**
     * Get all the multipliers for a player in the current server.
     *
     * @param uuid player to get multipliers from the database.
     * @return multipliers of the player in this server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getDatabase().getMultipliers(uuid, PLUGIN.getConfig().getServerName());
    }

    /**
     * Get all multipliers for a player in the specified server.
     *
     * @param uuid   player to get multipliers from the database.
     * @param server where we should get the multipliers.
     * @return multipliers of the player in that server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid, @Nonnull String server) {
        return PLUGIN.getDatabase().getMultipliers(uuid, server);
    }

    public static CoinsPlugin getPlugin() {
        return PLUGIN;
    }

    public static void setPlugin(@Nonnull CoinsPlugin plugin) {
        PLUGIN = plugin;
    }

    public static void deletePlugin() {
        PLUGIN = null;
    }
}
