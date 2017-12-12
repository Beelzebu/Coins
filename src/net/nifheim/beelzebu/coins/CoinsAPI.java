/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
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
package net.nifheim.beelzebu.coins;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import net.nifheim.beelzebu.coins.CoinsResponse.CoinsResponseType;
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.utils.CacheManager;

/**
 *
 * @author Beelzebu
 */
public class CoinsAPI {

    private static final Core core = Core.getInstance();
    private static final DecimalFormat DF = new DecimalFormat("#.#");

    /**
     * Get the coins of a Player by his name.
     *
     * @param player Player to get the coins.
     * @return
     */
    public static double getCoins(String player) {
        if (CacheManager.getCoins(core.getUUID(player)) == -1) {
            double coins = core.getDatabase().getCoins(player);
            CacheManager.updateCoins(core.getUUID(player), coins);
        }
        return CacheManager.getCoins(core.getUUID(player));
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return
     */
    public static double getCoins(UUID uuid) {
        if (CacheManager.getCoins(uuid) == -1) {
            double coins = core.getDatabase().getCoins(uuid);
            CacheManager.updateCoins(uuid, coins);
        }
        return CacheManager.getCoins(uuid);
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param p Player to get the coins string.
     * @return
     */
    public static String getCoinsString(String p) {
        double coins = getCoins(p);
        if (coins > -1 && isindb(p)) {
            return (DF.format(coins));
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Get the coins String of a player by his name.
     *
     * @param p Player to get the coins string.
     * @return
     */
    public static String getCoinsString(UUID p) {
        double coins = getCoins(p);
        if (coins > -1 && isindb(p)) {
            return (DF.format(coins));
        } else {
            return "This player isn't in the database";
        }
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param player The player to add the coins.
     * @param coins The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return the response from the database.
     */
    public static CoinsResponse addCoins(String player, double coins, boolean multiply) {
        if (!isindb(player)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + player + " isn't in the database.");
        }
        double finalCoins = coins;
        if (multiply) {
            finalCoins *= getMultiplier().getAmount();
            for (String perm : core.getMethods().getPermissions(core.getUUID(player))) {
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
        finalCoins += getCoins(player);
        core.getDatabase().setCoins(player, finalCoins);
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
    public static CoinsResponse addCoins(UUID uuid, double coins, boolean multiply) {
        if (!isindb(uuid)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
        double finalCoins = coins;
        if (multiply) {
            coins *= getMultiplier().getAmount();
            for (String perm : core.getMethods().getPermissions(uuid)) {
                if (perm.startsWith("coins.multiplier.x")) {
                    try {
                        int i = Integer.parseInt(perm.split("coins.multiplier.x")[1]);
                        coins *= i;
                        break;
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        finalCoins += getCoins(uuid);
        core.getDatabase().setCoins(uuid, finalCoins);
        return new CoinsResponse(CoinsResponseType.SUCCESS, "");
    }

    /**
     * Take coins of a player by his name.
     *
     * @param name The name of the player to take the coins.
     * @param coins
     */
    public static void takeCoins(String name, double coins) {
        double finalCoins = getCoins(name) - coins;
        core.getDatabase().setCoins(name, finalCoins);
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param uuid The UUID of the player to take the coins.
     * @param coins
     */
    public static void takeCoins(UUID uuid, double coins) {
        double finalCoins = getCoins(uuid) - coins;
        core.getDatabase().setCoins(uuid, finalCoins);
        core.getDatabase().setCoins(uuid, coins);
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param name The name of the player to reset the coins.
     * @return The response from the Database.
     */
    public static CoinsResponse resetCoins(String name) {
        if (isindb(name)) {
            core.getDatabase().setCoins(name, core.getConfig().getDouble("General.Starting Coins", 0));
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
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
            core.getDatabase().setCoins(uuid, core.getConfig().getDouble("General.Starting Coins", 0));
            return new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "The player " + uuid + " isn't in the database.");
        }
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param p
     * @param coins
     */
    public static void setCoins(String p, double coins) {
        core.getDatabase().setCoins(p, coins);
    }

    /**
     * Set the coins of a player by his name.
     *
     * @param p
     * @param coins
     */
    public static void setCoins(UUID p, double coins) {
        core.getDatabase().setCoins(p, coins);
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
     * @param player The name to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(String player) {
        if (CacheManager.getCoins(core.getUUID(player)) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return core.getDatabase().isindb(player);
    }

    /**
     * Get if a player with the specified uuid exists in the database.
     *
     * @param uuid The uuid to look for in the database.
     * @return true if the player exists in the database or false if not.
     */
    public static boolean isindb(UUID uuid) {
        if (CacheManager.getCoins(uuid) > -1) { // If the player is in the cache it should be in the database.
            return true;
        }
        return core.getDatabase().isindb(uuid);
    }

    /**
     * Get the top players in coins data.
     *
     * @param top The lenght of the top list, for example 5 will get a max of 5
     * users for the top.
     * @return The ordered top list of players and his balance.
     */
    public static Map<String, Double> getTopPlayers(int top) {
        return core.getDatabase().getTopPlayers(top);
    }

    /**
     * Register a user in the database with the default starting balance.
     *
     * @param nick The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static void createPlayer(String nick, UUID uuid) {
        createPlayer(nick, uuid, core.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Register a user in the database with the specified balance.
     *
     * @param nick The name of the user that will be registered.
     * @param uuid The uuid of the user.
     * @param balance The balance of the user.
     */
    public static void createPlayer(String nick, UUID uuid, double balance) {
        core.getDatabase().createPlayer(uuid, nick, balance);
    }

    /**
     * Get and modify information about multipliers for the specified server.
     *
     * @param server The server to modify and get info about multiplier.
     * @return The active multiplier for the specified server.
     */
    public static Multiplier getMultiplier(String server) {
        if (CacheManager.getMultiplier(server) == null) {
            CacheManager.addMultiplier(server, new Multiplier(server));
        }
        return CacheManager.getMultiplier(server);
    }

    /**
     * Get and modify information about the multiplier for the server specified
     * in the plugin's config or manage other data about multipliers.
     *
     * @return The active multiplier for this server.
     */
    public static Multiplier getMultiplier() {
        return getMultiplier(core.getConfig().getString("Multipliers.Server", "default"));
    }
}
