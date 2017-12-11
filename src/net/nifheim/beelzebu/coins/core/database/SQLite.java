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
package net.nifheim.beelzebu.coins.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.core.Core;
import static net.nifheim.beelzebu.coins.core.database.Database.prefix;

/**
 *
 * @author Beelzebu
 */
public class SQLite implements Database {

    private final Core core;
    private HikariDataSource ds;
    private Connection connection;

    public SQLite(Core c) {
        core = c;
        try {
            setupDatabase();
            updateDatabase();
        } catch (SQLException ex) {
            Logger.getLogger(SQLite.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    private void setupDatabase() throws SQLException {
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("Coins SQLite Connection Pool");
        hc.setJdbcUrl("jdbc:sqlite:plugins/Coins/database.db");
        hc.setConnectionTestQuery("SELECT 1");
        hc.setMaxLifetime(60000);
        hc.setMinimumIdle(4);
        hc.setIdleTimeout(30000);
        hc.setConnectionTimeout(10000);
        hc.setMaximumPoolSize(50);
        hc.setLeakDetectionThreshold(30000);
        hc.validate();
        ds = new HikariDataSource(hc);

        try (Connection c = ds.getConnection()) {
            if (c.isClosed()) {
                core.log("Can't connect to the database...");
            }
        } catch (SQLException ex) {
            core.log("Can't connect to the database...");
            core.debug(ex);
        }
    }

    private void updateDatabase() {
        try (Connection c = ds.getConnection()) {
            core.debug("A database connection was opened.");
            String Data
                    = "CREATE TABLE IF NOT EXISTS `Data`"
                    + "(`uuid` VARCHAR(50),"
                    + "`nick` VARCHAR(50),"
                    + "`balance` DOUBLE,"
                    + "`lastlogin` LONG);";
            String Multiplier = "CREATE TABLE IF NOT EXISTS `Multipliers`"
                    + "(`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`uuid` VARCHAR(50),"
                    + "`multiplier` INT,"
                    + "`queue` INT,"
                    + "`minutes` INT,"
                    + "`endtime` LONG,"
                    + "`server` VARCHAR(50),"
                    + "`enabled` BOOLEAN);";
            c.createStatement().executeUpdate(Data);
            core.debug("The data table was updated.");
            c.createStatement().executeUpdate(Multiplier);
            core.debug("The multipliers table was updated");
            if (core.getConfig().getBoolean("General.Purge.Enabled", true)) {
                c.createStatement().executeUpdate("DELETE FROM Data WHERE lastlogin < " + (System.currentTimeMillis() - (core.getConfig().getInt("General.Purge.Days") * 86400000)) + ";");
                core.debug("Inactive users were removed from the database.");
            }
        } catch (SQLException ex) {
            core.log("Something was wrong creating the default databases. Please check the debug log.");
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
    }

    @Override
    public void createPlayer(Connection c, String player, UUID uuid, double balance) {
        try {
            core.debug("A database connection was opened.");
            ResultSet res = null;
            try {
                core.debug("Trying to create or update data.");
                if (core.getConfig().getBoolean("Online Mode")) {
                    core.debug("Preparing to create or update an entry for online mode.");
                    res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
                    if (!res.next()) {
                        Utils.generatePreparedStatement(c, SQLQuery.CREATE_USER, uuid, player, balance, System.currentTimeMillis()).executeUpdate();
                        core.debug("An entry in the database was created for: " + player);
                    } else {
                        Utils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_ONLINE, player, System.currentTimeMillis(), uuid).executeUpdate();
                        core.debug("The nickname of: " + player + " was updated in the database.");
                    }
                } else {
                    core.debug("Preparing to create or update an entry for offline mode.");
                    res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, player).executeQuery();
                    if (!res.next()) {
                        Utils.generatePreparedStatement(c, SQLQuery.CREATE_USER, uuid, player, balance, System.currentTimeMillis()).executeUpdate();
                        core.debug("An entry in the database was created for: " + player);
                    } else {
                        Utils.generatePreparedStatement(c, SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), player).executeUpdate();
                        core.debug("The uuid of: " + core.getNick(uuid) + " was updated in the database.");
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
            core.log("&cAn internal error has occurred creating the player: " + player + " in the database.");
            core.debug(ex);
        }
    }

    @Override
    public Double getCoins(String player) {
        double coins = -1;
        try (Connection c = ds.getConnection(); ResultSet res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, player).executeQuery();) {
            if (CoinsAPI.isindb(player)) {
                res.next();
                coins = res.getDouble("balance");
            } else {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, player, core.getUUID(player), coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred creating the data for player: " + player);
            core.debug(ex);
        }
        return coins;
    }

    @Override
    public void addCoins(String player, Double coins) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, oldCoins + coins, player).executeUpdate();
                core.updateCache(core.getUUID(player), oldCoins + coins);
                core.getMethods().callCoinsChangeEvent(core.getUUID(player), oldCoins, oldCoins + coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred adding coins to the player: " + player);
            core.debug(ex);
        }
    }

    @Override
    public void takeCoins(String player, Double coins) {
        try (Connection c = ds.getConnection()) {
            double beforeCoins = getCoins(player);
            if ((beforeCoins - coins) < 0 || beforeCoins == coins) {
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, 0, player).executeUpdate();
                core.updateCache(core.getUUID(player), 0D);
            } else {
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, beforeCoins - coins, player).executeUpdate();
                core.updateCache(core.getUUID(player), getCoins(player));
            }
            core.getMethods().callCoinsChangeEvent(core.getUUID(player), beforeCoins, beforeCoins - coins);
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred taking coins to the player: " + player);
            core.debug(ex);
        }
    }

    @Override
    public void resetCoins(String player) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, core.getConfig().getDouble("General.Starting Coins", 0), player).executeUpdate();
                core.updateCache(core.getUUID(player), core.getConfig().getDouble("General.Starting Coins"));
                core.getMethods().callCoinsChangeEvent(core.getUUID(player), oldCoins, core.getConfig().getDouble("General.Starting Coins"));
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred reseting the coins of player: " + player);
            core.debug(ex);
        }
    }

    @Override
    public void setCoins(String player, Double coins) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_OFFLINE, coins, player).executeUpdate();
                core.updateCache(core.getUUID(player), coins);
                core.getMethods().callCoinsChangeEvent(core.getUUID(player), oldCoins, coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred setting the coins of player: " + player);
            core.debug(ex);
        }
    }

    @Override
    public boolean isindb(String player) {
        try (Connection c = ds.getConnection(); ResultSet res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, player).executeQuery()) {
            if (res.next()) {
                return res.getString("nick") != null;
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred cheking if the player: " + player + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }

    @Override
    public Double getCoins(UUID player) {
        double coins = -1;
        try (Connection c = ds.getConnection(); ResultSet res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, player).executeQuery()) {
            if (CoinsAPI.isindb(player)) {
                res.next();
                coins = res.getDouble("balance");
            } else {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, core.getNick(player), player, coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred creating the data for player: " + core.getNick(player));
            core.debug(ex);
        }
        return coins;
    }

    @Override
    public void addCoins(UUID player, Double coins) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, oldCoins + coins, player).executeUpdate();
                core.updateCache(player, oldCoins + coins);
                core.getMethods().callCoinsChangeEvent(player, oldCoins, oldCoins + coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred adding coins to the player: " + core.getNick(player));
            core.debug(ex);
        }
    }

    @Override
    public void takeCoins(UUID player, Double coins) {
        try (Connection c = ds.getConnection()) {
            double beforeCoins = getCoins(player);
            if ((beforeCoins - coins) < 0 || beforeCoins == coins) {
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, 0, player).executeUpdate();
                core.updateCache(player, 0D);
            } else {
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, beforeCoins - coins, player).executeUpdate();
                core.updateCache(player, getCoins(player));
            }
            core.getMethods().callCoinsChangeEvent(player, beforeCoins, beforeCoins - coins);
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred taking coins to the player: " + core.getNick(player));
            core.debug(ex);
        }
    }

    @Override
    public void resetCoins(UUID player) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, core.getConfig().getDouble("General.Starting Coins", 0), player).executeUpdate();
                core.updateCache(player, core.getConfig().getDouble("General.Starting Coins"));
                core.getMethods().callCoinsChangeEvent(player, oldCoins, core.getConfig().getDouble("General.Starting Coins"));
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred reseting the coins of player: " + core.getNick(player));
            core.debug(ex);
        }
    }

    @Override
    public void setCoins(UUID player, Double coins) {
        try (Connection c = ds.getConnection()) {
            if (CoinsAPI.isindb(player)) {
                double oldCoins = getCoins(player);
                Utils.generatePreparedStatement(c, SQLQuery.UPDATE_COINS_ONLINE, coins, player).executeUpdate();
                core.updateCache(player, coins);
                core.getMethods().callCoinsChangeEvent(player, oldCoins, coins);
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred setting the coins of player: " + core.getNick(player));
            core.debug(ex);
        }
    }

    @Override
    public boolean isindb(UUID player) {
        try (Connection c = ds.getConnection(); ResultSet res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, player).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            core.log("&cAn internal error has occurred cheking if the player: " + player + " exists in the database.");
            core.debug(ex);
        }
        return false;
    }
    
    @Override
    public Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topplayers = new HashMap<>();
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = Utils.generatePreparedStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();
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
        return Utils.sortByValue(topplayers);
    }

    private boolean isColumnMissing(DatabaseMetaData metaData, String table, String column) throws SQLException {
        try (ResultSet res = metaData.getColumns(null, null, prefix + table, column)) {
            return !res.next();
        }
    }

    @Override
    public String getNick(UUID uuid) {
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();
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
    public UUID getUUID(String nick) {
        try (Connection c = ds.getConnection()) {
            ResultSet res = null;
            try {
                res = Utils.generatePreparedStatement(c, SQLQuery.SEARCH_USER_OFFLINE, nick).executeQuery();
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
            core.log("Something was wrong getting the uuid for the nick '" + nick + "'");
            core.debug(ex);
        }
        return null;
    }

    @Override
    public Map<String, Double> getAllPlayers() {
        Map<String, Double> data = new HashMap<>();
        try (Connection c = ds.getConnection(); ResultSet res = c.prepareStatement("SELECT * FROM " + Core.getInstance().getConfig().getString("MySQL.Prefix") + "Data;").executeQuery()) {
            while (res.next()) {
                data.put(res.getString("nick") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            core.debug("An error has ocurred getting all the players from the database.");
            core.debug(ex);
        }
        return data;
    }

    @Override
    public void shutdown() {
        ds.close();
    }
}
