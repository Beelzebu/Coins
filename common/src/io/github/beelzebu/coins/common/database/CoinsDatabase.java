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
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.utils.database.DatabaseUtils;
import io.github.beelzebu.coins.common.utils.database.SQLQuery;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class CoinsDatabase {

    protected static final CoinsCore CORE = CoinsCore.getInstance();
    protected static final String PREFIX = CORE.getDatabase() instanceof MySQL ? CORE.getConfig().getString("MySQL.Prefix") : "";
    public static final String DATA_TABLE = PREFIX + CORE.getConfig().getString("MySQL.Data Table", "data");
    public static final String MULTIPLIERS_TABLE = PREFIX + CORE.getConfig().getString("MySQL.Multipliers Table", "multipliers");
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
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                coins = res.getDouble("balance");
            } else if (CORE.getBootstrap().isOnline(uuid)) {
                coins = CORE.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, uuid, CORE.getNick(uuid, false).toLowerCase(), coins);
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has occurred creating the data for player: " + uuid);
            CORE.debug(ex);
        }
        return coins;
    }

    public final CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || isindb(uuid)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_COINS_ONLINE, amount, uuid).executeUpdate();
                CORE.getMessagingService().publishUser(uuid, amount);
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.SUCCESS, "");
            } else {
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "This user isn't in the database or the cache.");
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            CORE.log("An internal error has occurred setting coins to the player: " + uuid);
            CORE.debug(ex);
        }
        return response;
    }

    public final boolean isindb(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has occurred cheking if the player: " + uuid + " exists in the database.");
            CORE.debug(ex);
        }
        return false;
    }

    public final boolean isindb(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has occurred cheking if the player: " + name + " exists in the database.");
            CORE.debug(ex);
        }
        return false;
    }

    public final void createPlayer(UUID uuid, String name, double balance) {
        try (Connection c = getConnection()) {
            createPlayer(c, uuid, name, balance);
        } catch (SQLException ex) {
            CORE.log("An internal error has ocurred while creating the player " + name + " in the database, check the logs for more info.");
            CORE.debug(ex);
        }

    }

    public final void createPlayer(Connection c, UUID uuid, String name, double balance) {
        Preconditions.checkNotNull(uuid, "Can't create a player with null UUID");
        Preconditions.checkNotNull(name, "Can't create a player with null name");
        if (CoinsAPI.isindb(uuid)) {
            return;
        }
        try {
            try (ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
                CORE.debug("Creating data for player: " + name + " in the database.");
                if (!res.next()) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
                    CORE.debug("An entry in the database was created for: " + name);
                }
            } finally {
                c.close();
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has occurred creating the player: " + name + " in the database.");
            CORE.debug(ex);
        }
    }

    public final void updatePlayer(UUID uuid, String name) {
        try (Connection c = getConnection()) {
            updatePlayer(c, uuid, name);
        } catch (SQLException ex) {
            CORE.log("An internal error has ocurred updating the data for player '" + name + "', check the logs for more info.");
            CORE.debug(ex);
        }
    }

    public final void updatePlayer(Connection c, UUID uuid, String name) {
        try {
            try {
                if (CORE.getConfig().isOnline() && CoinsAPI.isindb(uuid)) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_ONLINE, name, System.currentTimeMillis(), uuid).executeUpdate();
                    CORE.debug("Updated the name for '" + uuid + "' (" + name + ")");
                } else if (!CORE.getConfig().isOnline() && CoinsAPI.isindb(name)) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), name).executeUpdate();
                    CORE.debug("Updated the UUID for '" + name + "' (" + uuid + ")");
                } else if (CORE.getBootstrap().isOnline(name) && !CoinsAPI.isindb(name)) {
                    CORE.debug(name + " isn't in the database, but is online and a plugin is requesting his balance.");
                    CoinsAPI.createPlayer(name, uuid);
                } else {
                    CORE.debug("Tried to update a player that isn't in the database and is offline.");
                }
            } finally {
                c.close();
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has ocurred updating the data for player '" + name + "'");
            CORE.debug(ex);
        }
    }

    public final LinkedHashMap<String, Double> getTopPlayers(int top) {
        LinkedHashMap<String, Double> topplayers = new LinkedHashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();) {
            while (res.next()) {
                String playername = res.getString("nick");
                double coins = res.getDouble("balance");
                topplayers.put(playername, coins);
            }
        } catch (SQLException ex) {
            CORE.log("An internal error has occurred generating the toplist");
            CORE.debug(ex);
        }
        return DatabaseUtils.sortByValue(topplayers);
    }

    public final String getNick(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
            if (res.next()) {
                return res.getString("nick");
            }
        } catch (SQLException ex) {
            CORE.log("Something was wrong getting the nick for the uuid '" + uuid + "'");
            CORE.debug(ex);
        }
        return null;
    }

    public final UUID getUUID(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return UUID.fromString(res.getString("uuid"));
            }
        } catch (SQLException ex) {
            CORE.log("Something was wrong getting the uuid for the nick '" + name + "'");
            CORE.debug(ex);
        }
        return null;
    }

    public final void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_MULTIPLIER, server, uuid, type, amount, minutes, 0, false, false).executeUpdate();
        } catch (SQLException ex) {
            CORE.log("Something was wrong when creating a multiplier for " + CORE.getNick(uuid, false));
            CORE.debug(ex);
        }
    }

    public final void deleteMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.DELETE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            CORE.log("An error has ocurred while deleting the multiplier #" + multiplier.getId());
            CORE.debug(ex);
        }
    }

    public final void enableMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.ENABLE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            CORE.log("An error has ocurred enabling the multiplier #" + multiplier.getId());
            CORE.debug(ex);
        }
    }

    public final Set<Multiplier> getMultipliers(UUID uuid) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS, uuid).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            CORE.log("An error has ocurred getting all the multipliers for " + uuid);
            CORE.debug(ex);
        }
        return multipliers;
    }

    public final Set<Multiplier> getMultipliers(UUID uuid, String server) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_SERVER, uuid, server).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            CORE.log("An error has ocurred getting all the multipliers for " + uuid + " in server " + server);
            CORE.debug(ex);
        }
        return multipliers;
    }

    public final Multiplier getMultiplier(int id) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_MULTIPLIER, id).executeQuery()) {
            if (res.next()) {
                return MultiplierBuilder.newBuilder(res.getString("server"), MultiplierType.valueOf(res.getString("type")), new MultiplierData(UUID.fromString(res.getString("uuid")), CORE.getNick(UUID.fromString(res.getString("uuid")), false), res.getInt("amount"), res.getInt("minutes"))).setID(res.getInt("id")).setEnabled(res.getBoolean("enabled")).setQueue(res.getBoolean("queue")).build(false);
            }
        } catch (SQLException ex) {
            CORE.log("An error has ocurred getting the multiplier with the id #" + id + " from the database.");
            CORE.debug(ex);
        }
        return null;
    }

    public final LinkedHashMap<String, Double> getAllPlayers() {
        LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_PLAYERS).executeQuery()) {
            while (res.next()) {
                data.put(res.getString("nick") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            CORE.log("An error has ocurred getting all the players from the database, check the logs for more info.");
            CORE.debug(ex);
        }
        return data;
    }

    public final void shutdown() {
        ds.close();
    }
}
