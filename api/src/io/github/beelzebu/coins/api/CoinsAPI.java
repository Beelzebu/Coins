/*
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
import io.github.beelzebu.coins.api.utils.CoinsEntry;
import io.github.beelzebu.coins.api.utils.CoinsSet;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoinsAPI {

    private static final DecimalFormat DF = new DecimalFormat("#.#");
    private static final CoinsEntry<CoinsSet<CoinsTopEntry>, Long> CACHED_TOP = new CoinsEntry<>(new CoinsSet<>(), -1L);
    private static final long TOP_CACHE_MILLIS = 30000;
    private static CoinsPlugin PLUGIN = null;

    /**
     * Get the coins of a Player by his name.
     *
     * @param name Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull String name) {
        UUID uuid = PLUGIN.getUniqueId(name, false);
        return PLUGIN.getCache().getCoins(PLUGIN.getUniqueId(name, false)).orElseGet(() -> {
            PLUGIN.getStorageProvider().updatePlayer(uuid, name);
            return PLUGIN.getStorageProvider().getCoins(PLUGIN.getUniqueId(name, false));
        });
    }

    /**
     * Get the coins of a Player by his UUID.
     *
     * @param uuid Player to get the coins.
     * @return coins of the player
     */
    public static double getCoins(@Nonnull UUID uuid) {
        return PLUGIN.getCache().getCoins(uuid).orElseGet(() -> {
            PLUGIN.getStorageProvider().updatePlayer(uuid, PLUGIN.getName(uuid, false));
            return PLUGIN.getStorageProvider().getCoins(uuid);
        });
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
            return PLUGIN.getString("Errors.Unknown player2", "");
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
            return PLUGIN.getString("Errors.Unknown player2", "");
        }
    }

    /**
     * Add coins to a player by his name, selecting if the multipliers should be used to calculate the coins.
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
     * Add coins to a player by his UUID, selecting if the multipliers should be used to calculate the coins.
     *
     * @param uuid     Player to add the coins.
     * @param coins    Coins to add.
     * @param multiply Multiply coins if there are any active multipliers
     * @return {@link io.github.beelzebu.coins.api.CoinsResponse}
     */
    public static CoinsResponse addCoins(@Nonnull UUID uuid, double coins, boolean multiply) {
        if (!isindb(uuid)) {
            return new CoinsResponse(CoinsResponseType.FAILED, "Errors.Unknown player2");
        }
        double finalCoins = coins;
        if (multiply && !getMultipliers().isEmpty()) {
            int multiplyTotal = getMultipliers().stream().filter(Multiplier::isEnabled).mapToInt(multiplier -> {
                if (multiplier.getType().equals(MultiplierType.PERSONAL) && !Objects.equals(uuid, multiplier.getData().getEnablerUUID())) {
                    return 0;
                }
                return multiplier.getData().getAmount();
            }).sum();
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
            if (Double.isNaN(coins) || Double.isInfinite(coins) || new BigDecimal(coins).compareTo(new BigDecimal(Double.MAX_VALUE)) > 0) {
                PLUGIN.log("An API call tried to exceed the max amount of coins that a account can handle.");
                PLUGIN.log(PLUGIN.getStackTrace(new IllegalArgumentException()));
                return new CoinsResponse(CoinsResponseType.FAILED, "Errors.Max value exceeded");
            }
            PLUGIN.getMessagingService().publishUser(uuid, coins);
            return PLUGIN.getStorageProvider().setCoins(uuid, coins);
        } else {
            return new CoinsResponse(CoinsResponseType.FAILED, "Errors.Unknown player2");
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
        return new CoinsResponse(CoinsResponseType.FAILED, "Errors.No Coins");
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
        return new CoinsResponse(CoinsResponseType.FAILED, "Errors.No Coins");
    }

    /**
     * Get if a player with the specified name exists in the storageProvider. Is not recommended check a player by his
     * name because it can change.
     *
     * @param name The name to look for in the storageProvider.
     * @return true if the player exists in the storageProvider or false if not.
     */
    public static boolean isindb(@Nonnull String name) {
        UUID uuid = PLUGIN.getUniqueId(name, false);
        if (uuid != null && PLUGIN.getCache().getCoins(uuid).isPresent()) { // If the player is in the cache it should be in the storageProvider.
            return true;
        }
        boolean exists = PLUGIN.getStorageProvider().isindb(name);
        if (!exists && PLUGIN.getBootstrap().isOnline(name)) {
            createPlayer(name, uuid);
        }
        return exists;
    }

    /**
     * Get if a player with the specified uuid exists in the storageProvider.
     *
     * @param uuid The uuid to look for in the storageProvider.
     * @return true if the player exists in the storageProvider or false if not.
     */
    public static boolean isindb(@Nonnull UUID uuid) {
        if (PLUGIN.getCache().getCoins(uuid).isPresent()) { // If the player is in the cache it should be in the storageProvider.
            return true;
        }
        boolean exists = PLUGIN.getStorageProvider().isindb(uuid);
        if (!exists && PLUGIN.getBootstrap().isOnline(uuid)) {
            createPlayer(PLUGIN.getName(uuid, false), uuid);
        }
        return exists;
    }

    /**
     * Get the top players from the cache or the database, this method will try to get the top from the cache, if the
     * cache is older than {@link CoinsAPI#TOP_CACHE_MILLIS}, then it will try to get it from the database and update
     * the cache, if the amount of cached players is less than the requested players it will get the players from the
     * database too.
     *
     * @param top The length of the top list, for example "5" will get a max of 5 users for the top.
     * @return Array with the requested players.
     */
    public static CoinsTopEntry[] getTopPlayers(int top) {
        if (CACHED_TOP.getValue() != null && CACHED_TOP.getValue() >= System.currentTimeMillis() && CACHED_TOP.getKey().size() <= top) {
            return CACHED_TOP.getKey().getFirst(top).toArray(new CoinsTopEntry[top]);
        } else {
            CACHED_TOP.setKey(new CoinsSet<>(PLUGIN.getStorageProvider().getTopPlayers(top)));
            CACHED_TOP.setValue(System.currentTimeMillis() + TOP_CACHE_MILLIS);
            return CACHED_TOP.getKey().toArray(new CoinsTopEntry[top]);
        }
    }

    /**
     * Register a user in the storageProvider with the default starting balance.
     *
     * @param name The name of the user that will be registered.
     * @param uuid The uuid of the user.
     */
    public static void createPlayer(@Nonnull String name, UUID uuid) {
        createPlayer(name, uuid, PLUGIN.getConfig().getDouble("General.Starting Coins", 0));
    }

    /**
     * Register a user in the storageProvider with the specified balance.
     *
     * @param name    The name of the user that will be registered.
     * @param uuid    The uuid of the user.
     * @param balance The balance of the user.
     */
    public static void createPlayer(@Nonnull String name, UUID uuid, double balance) {
        PLUGIN.getStorageProvider().createPlayer(uuid, name, balance);
    }

    /**
     * Get all enabled multipliers for this server.
     *
     * @return The active multiplier for this server.
     */
    public static Set<Multiplier> getMultipliers() {
        return getMultipliers(PLUGIN.getConfig().getString("Multipliers.Server", "default"));
    }

    public static Set<Multiplier> getMultipliers(MultiplierFilter filter) {
        return getMultipliers().stream().filter(filter.getPredicate()).collect(Collectors.toSet());
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
     * Get a multiplier from the storageProvider by his ID and add it to the cache.
     *
     * @param id The ID of the multiplier.
     * @return The multiplier from the Cache.
     */
    public static Multiplier getMultiplier(int id) {
        return PLUGIN.getCache().getMultiplier(id).orElse(PLUGIN.getStorageProvider().getMultiplier(id));
    }

    /**
     * Get all multipliers for a player from the storageProvider.
     *
     * @param uuid player to get the multipliers from the storageProvider.
     * @return all multipliers that this player have.
     */
    public static Set<Multiplier> getAllMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid);
    }

    /**
     * Get all the multipliers for a player in the current server.
     *
     * @param uuid player to get multipliers from the storageProvider.
     * @return multipliers of the player in this server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid, PLUGIN.getConfig().getServerName());
    }

    /**
     * Get all multipliers for a player in the specified server.
     *
     * @param uuid   player to get multipliers from the storageProvider.
     * @param server where we should get the multipliers.
     * @return multipliers of the player in that server.
     */
    public static Set<Multiplier> getMultipliersFor(@Nonnull UUID uuid, @Nonnull String server) {
        return PLUGIN.getStorageProvider().getMultipliers(uuid, server);
    }

    public static Multiplier createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        Multiplier multiplier = MultiplierBuilder.newBuilder(server, type, new MultiplierData(uuid, PLUGIN.getName(uuid, false), amount, minutes)).build(false);
        PLUGIN.getStorageProvider().saveMultiplier(multiplier);
        return multiplier;
    }

    public static Multiplier createMultiplier(int amount, int minutes, String server, MultiplierType type) {
        Multiplier multiplier = MultiplierBuilder.newBuilder(server, type, new MultiplierData(amount, minutes)).build(false);
        PLUGIN.getStorageProvider().saveMultiplier(multiplier);
        return multiplier;
    }

    public static CoinsPlugin getPlugin() {
        return PLUGIN;
    }

    public static void setPlugin(@Nonnull CoinsPlugin plugin) {
        if (PLUGIN == null) {
            PLUGIN = plugin;
        }
    }

    public static void deletePlugin() {
        PLUGIN = null;
    }
}
