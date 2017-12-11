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

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
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
     */
    public static void addCoins(String player, double coins, boolean multiply) {
        if (multiply) {
            coins *= getMultiplier().getAmount();
            for (String perm : core.getMethods().getPermissions(core.getUUID(player))) {
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
        core.getDatabase().addCoins(player, coins);
    }

    /**
     * Add coins to a player by his UUID, selecting if the multipliers should be
     * used to calculate the coins.
     *
     * @param uuid The player to add the coins.
     * @param coins The coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     */
    public static void addCoins(UUID uuid, double coins, boolean multiply) {
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
        core.getDatabase().addCoins(uuid, coins);
    }

    /**
     * Take coins of a player by his name.
     *
     * @param p
     * @param coins
     */
    public static void takeCoins(String p, double coins) {
        core.getDatabase().takeCoins(p, coins);
    }

    /**
     * Take coins of a player by his UUID.
     *
     * @param p
     * @param coins
     */
    public static void takeCoins(UUID p, double coins) {
        core.getDatabase().takeCoins(p, coins);
    }

    /**
     * Reset the coins of a player by his name.
     *
     * @param p
     */
    public static void resetCoins(String p) {
        core.getDatabase().resetCoins(p);
    }

    /**
     * Reset the coins of a player by his UUID.
     *
     * @param p
     */
    public static void resetCoins(UUID p) {
        core.getDatabase().resetCoins(p);
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
    public static boolean payCoins(String from, String to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return true;
        }
        return false;
    }

    /**
     * Pay coins to another player.
     *
     * @param from The player to get the coins.
     * @param to The player to pay.
     * @param amount The amount of coins to pay.
     * @return true or false if the transaction is completed.
     */
    public static boolean payCoins(UUID from, UUID to, double amount) {
        if (getCoins(from) >= amount) {
            takeCoins(from, amount);
            addCoins(to, amount, false);
            return true;
        }
        return false;
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
        try {
            core.getDatabase().createPlayer(core.getDatabase().getConnection(), nick, uuid, balance);
        } catch (SQLException ex) {
            core.log("An error has ocurred while creating a player in the database from the API.");
            core.debug(ex);
        }
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
