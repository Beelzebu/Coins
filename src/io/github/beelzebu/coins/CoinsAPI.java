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
package io.github.beelzebu.coins;

import io.github.beelzebu.coins.CoinsResponse.CoinsResponseType;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoinsAPI {

    private static final CoinsCore CORE = CoinsCore.getInstance();
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    /**
     * Get the coins of a Player by his name.
     *
     * @param name Player to get the coins.
     * @return
     */
    public static double getCoins(String name) {
        return CacheManager.getCoins(CORE.getUUID(name, false));
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return
     */
    public static double getCoins(UUID uuid) {
        return CacheManager.getCoins(uuid);
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param name Player to get the coins string.
     * @return The coins in decimal format "#.#"
     */
    public static String getCoinsString(String name) {
        double coins = getCoins(name.toLowerCase());
        if (coins >= 0) {
            return (DF.format(coins));
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param uuid Player to get the coins string.
     * @return The coins in decimal format "#.#"
     */
    public static String getCoinsString(UUID uuid) {
        double coins = getCoins(uuid);
        if (coins >= 0) {
            return (DF.format(coins));
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param name The player to add the coins.
     * @param coins The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return the response from the database.
     */
    public static CoinsResponse addCoins(String name, final double coins, boolean multiply) {
        if (!isindb(name)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + name + " isn't in the database.");
        }
        double finalCoins = coins;
        if (multiply && getMultiplier() != null) {
            if (getMultiplier().getType().equals(MultiplierType.PERSONAL) && !getMultiplier().getEnablerUUID().equals(CORE.getUUID(name, false))) {
            } else {
                finalCoins *= getMultiplier().getAmount();
            }
            for (String perm : CORE.getBootstrap().getPermissions(CORE.getUUID(name, false))) {
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
        finalCoins += getCoins(name);
        CORE.getDatabase().setCoins(name.toLowerCase(), finalCoins);
        return new CoinsResponse(CoinsResponseType.SUCCESS, "");
    }

    /**
     * Add coins to a player by his UUID, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param uuid The player to add the coins.
     * @param coins The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return the response from the database.
     */
    public static CoinsResponse addCoins(UUID uuid, final double coins, boolean multiply) {
        if (!isindb(uuid)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
        double finalCoins = coins;
        if (multiply && getMultiplier() != null) {
            if (getMultiplier().getType().equals(MultiplierType.PERSONAL) && !getMultiplier().getEnablerUUID().equals(uuid)) {
            } else {
                finalCoins *= getMultiplier().getAmount();
            }
            for (String perm : CORE.getBootstrap().getPermissions(uuid)) {
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
        CORE.getDatabase().setCoins(uuid, finalCoins);
        return new CoinsResponse(CoinsResponseType.SUCCESS, "");
    }

    /**
     * Take coins of a player by his name.
     *
     * @param name The name of the player to take the coins.
     * @param coins the coins to take from the player.
     * @return The response from the Database.
     */
    public static CoinsResponse takeCoins(String name, double coins) {
        if (isindb(name)) {
            return CORE.getDatabase().setCoins(name.toLowerCase(), getCoins(name) - coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + name + " isn't in the database.");
        }
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param uuid The UUID of the player to take the coins.
     * @param coins the coins to take from the player.
     * @return The response from the Database.
     */
    public static CoinsResponse takeCoins(UUID uuid, double coins) {
        if (isindb(uuid)) {
            return CORE.getDatabase().setCoins(uuid, getCoins(uuid) - coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param name The name of the player to reset the coins.
     * @return The response from the Database.
     */
    public static CoinsResponse resetCoins(String name) {
        if (isindb(name)) {
            return CORE.getDatabase().setCoins(name.toLowerCase(), CORE.getConfig().getDouble("General.Starting Coins", 0));
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + name + " isn't in the database.");
        }
    }

    /**
     * Reset the coins of a player by his UUID.
     *
     * @param uuid The UUID of the player to reset the coins.
     * @return The response from the Database.
     */
    public static CoinsResponse resetCoins(UUID uuid) {
        if (isindb(uuid)) {
            CORE.getDatabase().setCoins(uuid, CORE.getConfig().getDouble("General.Starting Coins", 0));
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param name
     * @param coins
     * @return The response from the Database.
     */
    public static CoinsResponse setCoins(String name, double coins) {
        if (isindb(name)) {
            return CORE.getDatabase().setCoins(name.toLowerCase(), coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + name + " isn't in the database.");
        }
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param uuid
     * @param coins
     * @return The response from the Database.
     */
    public static CoinsResponse setCoins(UUID uuid, double coins) {
        if (isindb(uuid)) {
            return CORE.getDatabase().setCoins(uuid, coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
    }

    /**
     * Pay coins to another player.
     *
     * @param from The player to get the coins.
     * @param to The player to pay.
     * @param amount The amount of coins to pay.
     * @return true or false if the transaction is completed.
     */
    public static CoinsResponse payCoins(String from, String to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
        }
        return new CoinsResponse(CoinsResponseType.FAILED, "The user from doesn't have enought coins.");
    }

    /**
     * Pay coins to another player.
     *
     * @param from The player to get the coins.
     * @param to The player to pay.
     * @param amount The amount of coins to pay.
     * @return the response from the database.
     */
    public static CoinsResponse payCoins(UUID from, UUID to, double amount) {
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
    public static boolean isindb(String name) {
        UUID uuid = CORE.getUUID(name, false);
        if (CacheManager.getPlayersData().getIfPresent(uuid != null ? uuid : UUID.randomUUID()) != null && getCoins(name) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return CORE.getDatabase().isindb(name);
    }

    /**
     * Get if a player with the specified uuid exists in the database.
     *
     * @param uuid The uuid to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(UUID uuid) {
        if (CacheManager.getPlayersData().getIfPresent(uuid) != null && getCoins(uuid) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return CORE.getDatabase().isindb(uuid);
    }

    /**
     * Get the top players in coins data.
     *
     * @param top The lenght of the top list, for example 5 will get a max of 5
     * users for the top.
     * @return The ordered top list of players and his balance.
     */
    public static Map<String, Double> getTopPlayers(int top) {
        return CORE.getDatabase().getTopPlayers(top);
    }

    /**
     * Register a user in the database with the default starting balance.
     *
     * @param nick The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static void createPlayer(String nick, UUID uuid) {
        createPlayer(nick, uuid, CORE.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Register a user in the database with the specified balance.
     *
     * @param nick The name of the user that will be registered.
     * @param uuid The uuid of the user.
     * @param balance The balance of the user.
     */
    public static void createPlayer(String nick, UUID uuid, double balance) {
        CORE.getDatabase().createPlayer(uuid, nick, balance);
    }

    /**
     * Get the multiplier for this server from the cache if any exists.
     *
     * @param server The server to modify and get info about multiplier.
     * @return The active multiplier for the specified server can be null;
     */
    public static Multiplier getMultiplier(String server) {
        return CacheManager.getMultiplier(server);
    }

    /**
     * Get a multiplier from the database by his ID and add it to the cache.
     *
     * @param id The ID of the multiplier.
     * @return The multiplier from the Cache.
     */
    public static Multiplier getMultiplier(int id) {
        Multiplier multiplier = CORE.getDatabase().getMultiplier(id);
        if (multiplier != null) {
            CacheManager.addMultiplier(multiplier.getServer(), multiplier);
            return CacheManager.getMultiplier(multiplier.getServer());
        }
        return null;
    }

    /**
     * Get and modify information about the multiplier for the server specified
     * in the plugin's config.
     *
     * @return The active multiplier for this server.
     */
    public static Multiplier getMultiplier() {
        return getMultiplier(CORE.getConfig().getString("Multipliers.Server", "default"));
    }

    /**
     * Get all multipliers for a player from the database.
     *
     * @param uuid player to get the multipliers from the database.
     * @return all multipliers that this player have.
     */
    public static Set<Multiplier> getAllMultipliersFor(UUID uuid) {
        return CORE.getDatabase().getMultipliers(uuid);
    }

    /**
     * Get all the multipliers for a player in the current server.
     *
     * @param uuid player to get multipliers from the database.
     * @return multipliers of the player in this server.
     */
    public static Set<Multiplier> getMultipliersFor(UUID uuid) {
        return CORE.getDatabase().getMultipliers(uuid, CoinsCore.getInstance().getConfig().getServerName());
    }

    /**
     * Get all multipliers for a player in the specified server.
     *
     * @param uuid player to get multipliers from the database.
     * @param server where we should get the multipliers.
     * @return multipliers of the player in that server.
     */
    public static Set<Multiplier> getMultipliersFor(UUID uuid, String server) {
        return CORE.getDatabase().getMultipliers(uuid, server);
    }
}
