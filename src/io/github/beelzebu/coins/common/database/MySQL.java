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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.CoinsResponse;
import io.github.beelzebu.coins.CoinsResponse.CoinsResponseType;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.MultiplierBuilder;
import io.github.beelzebu.coins.MultiplierData;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.database.DatabaseUtils.SQLQuery;
import io.github.beelzebu.coins.common.utils.FileManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public class MySQL implements CoinsDatabase {

    private HikariDataSource ds;

    @Override
    public void setup() {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("Coins MySQL Connection Pool");
        hc.setDriverClassName("com.mysql.jdbc.Driver");
        hc.setJdbcUrl("jdbc:mysql://" + core.getConfig().getString("MySQL.Host") + ":" + core.getConfig().getString("MySQL.Port") + "/" + core.getConfig().getString("MySQL.Database") + "?autoReconnect=true&useSSL=false");
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("useServerPrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("characterEncoding", "utf8");
        hc.addDataSourceProperty("encoding", "UTF-8");
        hc.addDataSourceProperty("useUnicode", "true");
        hc.setUsername(core.getConfig().getString("MySQL.User"));
        hc.setPassword(core.getConfig().getString("MySQL.Password"));
        hc.setMaxLifetime(60000);
        hc.setMinimumIdle(4);
        hc.setIdleTimeout(30000);
        hc.setConnectionTimeout(10000);
        hc.setMaximumPoolSize(core.getConfig().getInt("MySQL.Connection Pool", 8));
        hc.setLeakDetectionThreshold(30000);
        hc.validate();
        ds = new HikariDataSource(hc);
        updateDatabase();
    }

    public void updateDatabase() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            core.debug("A database connection was opened.");
            try {
                String Data
                        = "CREATE TABLE IF NOT EXISTS `" + prefix + "data`"
                        + "(`id` INT NOT NULL AUTO_INCREMENT,"
                        + "`uuid` VARCHAR(50) NOT NULL,"
                        + "`nick` VARCHAR(50) NOT NULL,"
                        + "`balance` DOUBLE NOT NULL,"
                        + "`lastlogin` LONG NOT NULL,"
                        + "PRIMARY KEY (`id`));";
                String Multiplier = "CREATE TABLE IF NOT EXISTS `" + prefix + "multipliers`"
                        + "(`id` INT NOT NULL AUTO_INCREMENT,"
                        + "`server` VARCHAR(50),"
                        + "`uuid` VARCHAR(50) NOT NULL,"
                        + "`type` VARCHAR(20) NOT NULL,"
                        + "`amount` INT,"
                        + "`minutes` INT,"
                        + "`endtime` LONG,"
                        + "`queue` INT,"
                        + "`enabled` BOOLEAN,"
                        + "PRIMARY KEY (`id`));";
                st.executeUpdate(Data);
                core.debug("The data table was updated.");
                st.executeUpdate(Multiplier);
                core.debug("The multipliers table was updated");
                if (core.getConfig().getInt("Database Version", 1) < 2) {
                    try {
                        if (c.prepareStatement("SELECT * FROM " + prefix + "Data;").executeQuery().next() && !c.prepareStatement("SELECT * FROM " + prefix + "data;").executeQuery().next()) {
                            core.log("Seems that your database is outdated, we'll try to update this...");
                            ResultSet res = c.prepareStatement("SELECT * FROM " + prefix + "Data;").executeQuery();
                            while (res.next()) {
                                DatabaseUtils.generatePreparedStatement(c, SQLQuery.CREATE_USER, res.getString("uuid"), res.getString("nick"), res.getDouble("balance"), res.getLong("lastlogin")).executeUpdate();
                                core.debug("Migrated the data for " + res.getString("nick") + " (" + res.getString("uuid") + ")");
                            }
                            core.log("Successfully upadated database to version 2");
                        }
                        new FileManager(core).updateDatabaseVersion(2);
                    } catch (SQLException ex) {
                        for (int i = 0; i < 5; i++) {
                            core.log("An error has ocurred migrating the data from the old database, check the logs ASAP!");
                        }
                        core.debug(ex);
                        return;
                    }
                }
                if (core.getConfig().getBoolean("General.Purge.Enabled", true) && core.getConfig().getInt("General.Purge.Days") > 0) {
                    st.executeUpdate("DELETE FROM " + prefix + "data WHERE lastlogin < " + (System.currentTimeMillis() - (core.getConfig().getInt("General.Purge.Days", 60) * 86400000L)) + ";");
                    core.debug("Inactive users were removed from the database.");
                }
            } finally {
                st.close();
                c.close();
                core.debug("The connection was closed.");
            }
        } catch (SQLException ex) {
            core.log("Something was wrong creating the default databases. Please check the debug log.");
            core.debug(ex);
        }
    }

    @Override
    public double getCoins(UUID uuid) {
        double coins = -1;
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
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

    @Override
    public double getCoins(String name) {
        double coins = -1;
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery();) {
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

    @Override
    public CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || isindb(uuid)) {
                DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, amount, uuid);
                CacheManager.updateCache(uuid, amount);
                response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
            } else {
                response = new CoinsResponse(CoinsResponseType.FAILED, "This user isn't in the database or the cache.");
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("An internal error has occurred setting coins to the player: " + uuid);
            core.debug(ex);
        }
        return response;
    }

    @Override
    public CoinsResponse setCoins(String name, double amount) {
        CoinsResponse response;
        try (Connection c = ds.getConnection()) {
            DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, amount, name);
            response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("An internal error has occurred setting coins to the player: " + name);
            core.debug(ex);
        }
        return response;
    }

    @Override
    public boolean isindb(UUID uuid) {
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred cheking if the player: " + uuid + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    @Override
    public boolean isindb(String name) {
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            core.log("An internal error has occurred cheking if the player: " + name + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    @Override
    public void createPlayer(UUID uuid, String name, double balance) {
        try {
            createPlayer(ds.getConnection(), uuid, name, balance);
        } catch (SQLException ex) {
            core.log("An internal error has ocurred while creating the player " + name + " in the database, check the logs for more info.");
            core.debug(ex);
        }

    }

    public void createPlayer(Connection c, UUID uuid, String name, double balance) {
        Preconditions.checkNotNull(uuid, "Can't create a player with null UUID");
        Preconditions.checkNotNull(name, "Can't create a player with null name");
        if (CoinsAPI.isindb(uuid)) {
            return;
        }
        try {
            ResultSet res = DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
            try {
                core.debug("Creating data for player: " + name + " in the database.");
                if (!res.next()) {
                    DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
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

    @Override
    public void updatePlayer(UUID uuid, String name) {
        try {
            updatePlayer(ds.getConnection(), uuid, name);
        } catch (SQLException ex) {
            core.log("An internal error has ocurred updating the data for player '" + name + "', check the logs for more info.");
            core.debug(ex);
        }
    }

    public void updatePlayer(Connection c, UUID uuid, String name) {
        try {
            if (core.getConfig().isOnline() && CoinsAPI.isindb(uuid)) {
                DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_ONLINE, name, System.currentTimeMillis(), uuid).executeUpdate();
                core.debug("Updated the name for '" + uuid + "' (" + name + ")");
            } else if (!core.getConfig().isOnline() && CoinsAPI.isindb(name)) {
                DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), name).executeUpdate();
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

    @Override
    public Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topplayers = new HashMap<>();
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();) {
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

    @Override
    public String getNick(UUID uuid) {
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
            if (res.next()) {
                return res.getString("nick");
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the nick for the uuid '" + uuid + "'");
            core.debug(ex);
        }
        return null;
    }

    @Override
    public UUID getUUID(String name) {
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return UUID.fromString(res.getString("uuid"));
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the uuid for the nick '" + name + "'");
            core.debug(ex);
        }
        return null;
    }

    @Override
    public void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        try (Connection c = ds.getConnection()) {
            DatabaseUtils.generatePreparedStatement(c, SQLQuery.CREATE_MULTIPLIER, server, uuid, type, amount, minutes, 0, false, false).executeUpdate();
        } catch (SQLException ex) {
            core.log("Something was wrong when creating a multiplier for " + core.getNick(uuid, false));
            core.debug(ex);
        }
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        try (Connection c = ds.getConnection()) {
            c.prepareStatement("DELETE FROM " + prefix + "multipliers WHERE id = " + multiplier.getId() + ";").executeUpdate();
        } catch (SQLException ex) {
            core.log("An error has ocurred while deleting the multiplier #" + multiplier.getId());
            core.debug(ex);
        }
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        try (Connection c = ds.getConnection()) {
            c.prepareStatement("UPDATE " + prefix + "multipliers SET enabled = true WHERE id = " + multiplier.getId() + ";").executeUpdate();
        } catch (SQLException ex) {
            core.log("An error has ocurred enabling the multiplier #" + multiplier.getId());
            core.debug(ex);
        }
    }

    @Override
    public Set<Multiplier> getMultipliers(UUID uuid, boolean server) {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Connection c = ds.getConnection(); ResultSet res = c.prepareStatement("SELECT * FROM " + prefix + "multipliers WHERE uuid = '" + uuid + "' AND enabled = false AND queue = false" + (server ? " AND server = '" + core.getConfig().getServerName() + "'" : ";")).executeQuery()) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting all the multipliers for " + uuid);
            core.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Multiplier getMultiplier(int id) {
        try (Connection c = ds.getConnection(); ResultSet res = c.prepareStatement("SELECT * FROM " + prefix + "multipliers WHERE id = " + id + ";").executeQuery()) {
            if (res.next()) {
                return MultiplierBuilder.newBuilder(res.getString("server"), MultiplierType.valueOf(res.getString("type")), new MultiplierData(UUID.fromString(res.getString("uuid")), core.getNick(UUID.fromString(res.getString("uuid")), false), res.getInt("amount"), res.getInt("minutes"))).setID(res.getInt("id")).setEnabled(res.getBoolean("enabled")).setQueue(res.getBoolean("queue")).build(false);
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting the multiplier with the id #" + id + " from the database.");
            core.debug(ex);
        }
        return null;
    }

    @Override
    public Map<String, Double> getAllPlayers() {
        Map<String, Double> data = new HashMap<>();
        try (Connection c = ds.getConnection(); ResultSet res = c.prepareStatement("SELECT * FROM " + prefix + "data;").executeQuery()) {
            while (res.next()) {
                data.put(res.getString("nick") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            core.log("An error has ocurred getting all the players from the database, check the logs for more info.");
            core.debug(ex);
        }
        return data;
    }

    @Override
    public void shutdown() {
        ds.close();
    }
}
