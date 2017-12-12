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
package net.nifheim.beelzebu.coins.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.CoinsResponse;
import net.nifheim.beelzebu.coins.CoinsResponse.CoinsResponseType;
import net.nifheim.beelzebu.coins.database.DatabaseUtils.SQLQuery;

/**
 *
 * @author Beelzebu
 */
public class NewMySQL implements CoinsDatabase {

    private static HikariDataSource ds;

    @Override
    public void setup() {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("Coins MySQL Connection Pool");
        hc.setDriverClassName("com.mysql.jdbc.Driver");
        hc.setJdbcUrl("jdbc:mysql://" + core.getConfig().getString("MySQL.Host") + ":" + core.getConfig().getInt("MySQL.Port") + "/" + core.getConfig().getString("MySQL.Database") + "?autoReconnect=true");
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
        hc.setMaximumPoolSize(10);
        hc.setLeakDetectionThreshold(30000);
        hc.validate();
        ds = new HikariDataSource(hc);
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
                        + "`uuid` VARCHAR(50) NOT NULL,"
                        + "`multiplier` INT,"
                        + "`queue` INT,"
                        + "`minutes` INT,"
                        + "`endtime` LONG,"
                        + "`server` VARCHAR(50),"
                        + "`enabled` BOOLEAN,"
                        + "PRIMARY KEY (`id`));";
                st.executeUpdate(Data);
                core.debug("The data table was updated.");
                st.executeUpdate(Multiplier);
                core.debug("The multipliers table was updated");
                if (core.getConfig().getBoolean("General.Purge.Enabled", true) && core.getConfig().getInt("General.Purge.Days") > 0) {
                    st.executeUpdate("DELETE FROM " + prefix + "Data WHERE lastlogin < " + (System.currentTimeMillis() - (core.getConfig().getInt("General.Purge.Days", 60) * 86400000L)) + ";");
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
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, uuid).executeQuery();) {
            if (CoinsAPI.isindb(uuid)) {
                res.next();
                coins = res.getDouble("balance");
            } else {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, uuid, core.getNick(uuid), coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred creating the data for player: " + uuid);
            core.debug(ex);
        }
        return coins;
    }

    @Override
    public double getCoins(String name) {
        double coins = -1;
        try (Connection c = ds.getConnection(); ResultSet res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery();) {
            if (CoinsAPI.isindb(name)) {
                res.next();
                coins = res.getDouble("balance");
            } else {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, core.getUUID(name), name, coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred creating the data for player: " + name);
            core.debug(ex);
        }
        return coins;
    }

    @Override
    public CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || isindb(uuid)) {
                DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_ONLINE, uuid, amount);
                response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
            } else {
                response = new CoinsResponse(CoinsResponseType.FAILED, "This user isn't in the database or the cache.");
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("&cAn internal error has occurred setting coins to the player: " + uuid);
            core.debug(ex);
        }
        return response;
    }

    @Override
    public CoinsResponse setCoins(String name, double amount) {
        CoinsResponse response;
        try (Connection c = ds.getConnection()) {
            DatabaseUtils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_OFFLINE, name, amount);
            response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "An exception as ocurred with the database.");
            core.log("&cAn internal error has occurred setting coins to the player: " + name);
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
            core.log("&cAn internal error has occurred cheking if the player: " + uuid + " exists in the database.");
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
            core.log("&cAn internal error has occurred cheking if the player: " + name + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    @Override
    public void createPlayer(UUID uuid, String name, double balance) {
        try {
            createPlayer(ds.getConnection(), uuid, name, balance);
        } catch (SQLException ex) {
            core.log("&cAn internal error has ocurred while creating the player " + name + " in the database, check the logs for more info.");
            core.debug(ex);
        }

    }

    public void createPlayer(Connection c, UUID uuid, String name, double balance) {
        try {
            core.debug("A database connection was opened.");
            ResultSet res = null;
            try {
                core.debug("Trying to create or update data.");
                if (core.getConfig().getBoolean("Online Mode")) {
                    core.debug("Preparing to create or update an entry for online mode.");
                    res = DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
                    if (!res.next()) {
                        DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
                        core.debug("An entry in the database was created for: " + name);
                    } else {
                        DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.UPDATE_USER_ONLINE, name, System.currentTimeMillis(), uuid).executeUpdate();
                        core.debug("The nickname of: " + name + " was updated in the database.");
                    }
                } else {
                    core.debug("Preparing to create or update an entry for offline mode.");
                    res = DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery();
                    if (!res.next()) {
                        DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
                        core.debug("An entry in the database was created for: " + name);
                    } else {
                        DatabaseUtils.generatePreparedStatement(c, DatabaseUtils.SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), name).executeUpdate();
                        core.debug("The uuid of: " + name + " was updated in the database.");
                    }
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
                core.debug("The connection was closed.");
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred creating the player: " + name + " in the database.");
            core.debug(ex);
        }
    }

    @Override
    public Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topplayers = new HashMap<>();
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();
                while (res.next()) {
                    String playername = res.getString("nick");
                    double coins = res.getDouble("balance");
                    topplayers.put(playername, coins);
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred generating the toplist");
            core.debug(ex);
        }
        return DatabaseUtils.sortByValue(topplayers);
    }

    @Override
    public String getNick(UUID uuid) {
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
                if (res.next()) {
                    return res.getString("nick");
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the nick for the uuid '" + uuid + "'");
            core.debug(ex);
        }
        return null;
    }

    @Override
    public UUID getUUID(String name) {
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = DatabaseUtils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery();
                if (res.next()) {
                    return UUID.fromString(res.getString("uuid"));
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("Something was wrong getting the uuid for the nick '" + name + "'");
            core.debug(ex);
        }
        return null;
    }
}
