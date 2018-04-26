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
package io.github.beelzebu.coins.common.database;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.CoinsResponse;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.MultiplierBuilder;
import io.github.beelzebu.coins.MultiplierData;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.utils.database.DatabaseUtils;
import io.github.beelzebu.coins.common.utils.database.SQLQuery;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsDatabase {

    protected static final CoinsCore core = CoinsCore.getInstance();
    protected static final String PREFIX = core.getDatabase() instanceof MySQL ? core.getConfig().getString("MySQL.Prefix") : "";
    public static final String DATA_TABLE = PREFIX + core.getConfig().getString("MySQL.Data Table", "data");
    public static final String MULTIPLIERS_TABLE = PREFIX + core.getString("MySQL.Multipliers Table", "multipliers");
    protected HikariDataSource ds;

    public abstract void setup();

    private Connection getConnection() throws SQLException {
        if (ds != null && !ds.isClosed()) {
            return ds.getConnection();
        } else {
            shutdown();
            setup();
        }
        return ds.getConnection();
    }

    protected abstract void updateDatabase();

    public final double getCoins(UUID uuid) {
        double coins = -1;
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
            if (res.next()) {
                coins = res.getDouble("balance");
            } else if (core.getMethods().isOnline(uuid)) {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, uuid, core.getNick(uuid, false).toLowerCase(), coins);
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred creating the data for player: " + uuid);
            core.debug(ex);
        }
        return coins;
    }

    public final double getCoins(String name) {
        double coins = -1;
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery();) {
            if (res.next()) {
                coins = res.getDouble("balance");
            } else if (core.getMethods().isOnline(name)) {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, core.getUUID(name, false), name, coins);
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred creating the data for player: " + name);
            core.debug(ex);
        }
        return coins;
    }

    public final CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || isindb(uuid)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_COINS_ONLINE, amount, uuid);
                CacheManager.updateCache(uuid, amount);
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.SUCCESS, "");
            } else {
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "This user isn't in the database or the cache.");
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("An internal error has occurred setting coins to the player: " + uuid);
            core.debug(ex);
        }
        return response;
    }

    public final CoinsResponse setCoins(String name, double amount) {
        CoinsResponse response;
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, amount, name);
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.SUCCESS, "");
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("An internal error has occurred setting coins to the player: " + name);
            core.debug(ex);
        }
        return response;
    }

    public final boolean isindb(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred cheking if the player: " + uuid + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    public final boolean isindb(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred cheking if the player: " + name + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    public final void createPlayer(UUID uuid, String name, double balance) {
        try {
            createPlayer(getConnection(), uuid, name, balance);
        } catch (SQLException ex) {
            core.log("An internal error has ocurred while creating the player " + name + " in the database, check the logs for more info.");
            core.debug(ex);
        }

    }

    public final void createPlayer(Connection c, UUID uuid, String name, double balance) {
        Preconditions.checkNotNull(uuid, "Can't create a player with null UUID");
        Preconditions.checkNotNull(name, "Can't create a player with null name");
        if (CoinsAPI.isindb(uuid)) {
            return;
        }
        try {
            ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
            try {
                core.debug("Creating data for player: " + name + " in the database.");
                if (!res.next()) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
                    core.debug("An entry in the database was created for: " + name);
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
                core.debug("The connection was closed.");
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred creating the player: " + name + " in the database.");
            core.debug(ex);
        }
    }

    public final void updatePlayer(UUID uuid, String name) {
        try {
            updatePlayer(getConnection(), uuid, name);
        } catch (SQLException ex) {
            core.log("An internal error has ocurred updating the data for player '" + name + "', check the logs for more info.");
            core.debug(ex);
        }
    }

    public final void updatePlayer(Connection c, UUID uuid, String name) {
        try {
            if (core.getConfig().isOnline() && CoinsAPI.isindb(uuid)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_ONLINE, name, System.currentTimeMillis(), uuid).executeUpdate();
                core.debug("Updated the name for '" + uuid + "' (" + name + ")");
            } else if (!core.getConfig().isOnline() && CoinsAPI.isindb(name)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), name).executeUpdate();
                core.debug("Updated the UUID for '" + name + "' (" + uuid + ")");
            } else {
                core.debug("Tried to update a player that isn't in the database.");
            }
            c.close();
        } catch (SQLException ex) {
            core.log("An internal error has ocurred updating the data for player '" + name + "'");
            core.debug(ex);
        }
    }

    public final Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topplayers = new HashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();) {
            while (res.next()) {
                String playername = res.getString("nick");
                double coins = res.getDouble("balance");
                topplayers.put(playername, coins);
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred generating the toplist");
            core.debug(ex);
        }
        return DatabaseUtils.sortByValue(topplayers);
    }

    public final String getNick(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
            if (res.next()) {
                return res.getString("nick");
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the nick for the uuid '" + uuid + "'");
            core.debug(ex);
        }
        return null;
    }

    public final UUID getUUID(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return UUID.fromString(res.getString("uuid"));
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the uuid for the nick '" + name + "'");
            core.debug(ex);
        }
        return null;
    }

    public final void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_MULTIPLIER, server, uuid, type, amount, minutes, 0, false, false).executeUpdate();
        } catch (SQLException ex) {
            core.log("Something was wrong when creating a multiplier for " + core.getNick(uuid, false));
            core.debug(ex);
        }
    }

    public final void deleteMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.DELETE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            core.log("An error has ocurred while deleting the multiplier #" + multiplier.getId());
            core.debug(ex);
        }
    }

    public final void enableMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.ENABLE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            core.log("An error has ocurred enabling the multiplier #" + multiplier.getId());
            core.debug(ex);
        }
    }

    public final Set<Multiplier> getMultipliers(UUID uuid, boolean server) {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Connection c = getConnection(); ResultSet res = server ? DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_SERVER, uuid).executeQuery() : DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS, uuid).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting all the multipliers for " + uuid);
            core.debug(ex);
        }
        return multipliers;
    }

    public final Multiplier getMultiplier(int id) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_MULTIPLIER, id).executeQuery()) {
            if (res.next()) {
                return MultiplierBuilder.newBuilder(res.getString("server"), MultiplierType.valueOf(res.getString("type")), new MultiplierData(UUID.fromString(res.getString("uuid")), core.getNick(UUID.fromString(res.getString("uuid")), false), res.getInt("amount"), res.getInt("minutes"))).setID(res.getInt("id")).setEnabled(res.getBoolean("enabled")).setQueue(res.getBoolean("queue")).build(false);
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting the multiplier with the id #" + id + " from the database.");
            core.debug(ex);
        }
        return null;
    }

    public final Map<String, Double> getAllPlayers() {
        Map<String, Double> data = new HashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_PLAYERS).executeQuery()) {
            while (res.next()) {
                data.put(res.getString("nick") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting all the players from the database, check the logs for more info.");
            core.debug(ex);
        }
        return data;
    }

    public final void shutdown() {
        ds.close();
    }
}
