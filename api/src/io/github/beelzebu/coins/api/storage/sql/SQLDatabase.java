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
package io.github.beelzebu.coins.api.storage.sql;

import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.CoinsResponse;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.MultiplierBuilder;
import io.github.beelzebu.coins.api.MultiplierData;
import io.github.beelzebu.coins.api.MultiplierType;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageProvider;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public abstract class SQLDatabase implements StorageProvider {

    protected final CoinsPlugin plugin;
    protected final String prefix;
    protected final String dataTable;
    protected final String multipliersTable;
    protected HikariDataSource ds;

    public SQLDatabase(CoinsPlugin plugin) {
        this.plugin = plugin;
        prefix = plugin.getStorageType().equals(StorageType.SQLITE) ? "" : plugin.getConfig().getString("MySQL.Prefix");
        dataTable = prefix + plugin.getConfig().getString("MySQL.Data Table", "data");
        multipliersTable = prefix + plugin.getConfig().getString("MySQL.Multipliers Table", "multipliers");
    }


    protected String getDataTable() {
        return dataTable;
    }

    protected String getMultipliersTable() {
        return multipliersTable;
    }

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

    @Override
    public final double getCoins(UUID uuid) {
        double coins = -1;
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                coins = res.getDouble("balance");
            } else if (plugin.getBootstrap().isOnline(uuid)) {
                coins = plugin.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(c, uuid, plugin.getName(uuid, false).toLowerCase(), coins);
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred creating the data for player: " + uuid);
            plugin.debug(ex);
        }
        return coins;
    }

    @Override
    public final CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Connection c = getConnection()) {
            if (CoinsAPI.getCoins(uuid) > -1 || isindb(uuid)) {
                DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_COINS_ONLINE, amount, uuid).executeUpdate();
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.SUCCESS, "");
            } else {
                response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "This user isn't in the database or the cache.");
            }
        } catch (SQLException ex) {
            response = new CoinsResponse(CoinsResponse.CoinsResponseType.FAILED, "An exception as occurred with the database.");
            plugin.log("An internal error has occurred setting coins to the player: " + uuid);
            plugin.debug(ex);
        }
        return response;
    }

    @Override
    public final boolean isindb(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred cheking if the player: " + uuid + " exists in the database.");
            plugin.debug(ex);
        }
        return false;
    }

    @Override
    public final boolean isindb(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return res.getString("uuid") != null;
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred cheking if the player: " + name + " exists in the database.");
            plugin.debug(ex);
        }
        return false;
    }

    @Override
    public final void createPlayer(UUID uuid, String name, double balance) {
        try (Connection c = getConnection()) {
            createPlayer(c, uuid, name, balance);
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred while creating the player " + name + " in the database, check the logs for more info.");
            plugin.debug(ex);
        }

    }

    private final void createPlayer(Connection c, UUID uuid, String name, double balance) {
        Preconditions.checkNotNull(uuid, "Can't create a player with null UUID");
        Preconditions.checkNotNull(name, "Can't create a player with null name");
        if (CoinsAPI.isindb(uuid)) {
            return;
        }
        try {
            try (ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery()) {
                plugin.debug("Creating data for player: " + name + " in the database.");
                if (!res.next()) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_USER, uuid, name, balance, System.currentTimeMillis()).executeUpdate();
                    plugin.debug("An entry in the database was created for: " + name);
                }
            } finally {
                c.close();
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred creating the player: " + name + " in the database.");
            plugin.debug(ex);
        }
    }

    @Override
    public final void updatePlayer(UUID uuid, String name) {
        try (Connection c = getConnection()) {
            updatePlayer(c, uuid, name);
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred updating the data for player '" + name + "', check the logs for more info.");
            plugin.debug(ex);
        }
    }

    private final void updatePlayer(Connection c, UUID uuid, String name) {
        try {
            try {
                if (plugin.getConfig().isOnline() && CoinsAPI.isindb(uuid)) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_ONLINE, name, System.currentTimeMillis(), uuid).executeUpdate();
                    plugin.debug("Updated the name for '" + uuid + "' (" + name + ")");
                } else if (!plugin.getConfig().isOnline() && CoinsAPI.isindb(name)) {
                    DatabaseUtils.prepareStatement(c, SQLQuery.UPDATE_USER_OFFLINE, uuid, System.currentTimeMillis(), name).executeUpdate();
                    plugin.debug("Updated the UUID for '" + name + "' (" + uuid + ")");
                } else if (plugin.getBootstrap().isOnline(name) && !CoinsAPI.isindb(name)) {
                    plugin.debug(name + " isn't in the database, but is online and a plugin is requesting his balance.");
                    CoinsAPI.createPlayer(name, uuid);
                } else {
                    plugin.debug("Tried to update a player that isn't in the database and is offline.");
                }
            } finally {
                c.close();
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred updating the data for player '" + name + "'");
            plugin.debug(ex);
        }
    }

    @Override
    public final LinkedHashMap<String, Double> getTopPlayers(int top) {
        LinkedHashMap<String, Double> topplayers = new LinkedHashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_TOP, top).executeQuery();) {
            while (res.next()) {
                String playername = res.getString("nick");
                double coins = res.getDouble("balance");
                topplayers.put(playername, coins);
            }
        } catch (SQLException ex) {
            plugin.log("An internal error has occurred generating the toplist");
            plugin.debug(ex);
        }
        return DatabaseUtils.sortByValue(topplayers);
    }

    @Override
    public final String getName(UUID uuid) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_ONLINE, uuid).executeQuery();) {
            if (res.next()) {
                return res.getString("nick");
            }
        } catch (SQLException ex) {
            plugin.log("Something was wrong getting the nick for the uuid '" + uuid + "'");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public final UUID getUUID(String name) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SEARCH_USER_OFFLINE, name).executeQuery()) {
            if (res.next()) {
                return UUID.fromString(res.getString("uuid"));
            }
        } catch (SQLException ex) {
            plugin.log("Something was wrong getting the uuid for the nick '" + name + "'");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public final void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.CREATE_MULTIPLIER, server, uuid, type, amount, minutes, 0, false, false).executeUpdate();
        } catch (SQLException ex) {
            plugin.log("Something was wrong when creating a multiplier for " + plugin.getName(uuid, false));
            plugin.debug(ex);
        }
    }

    @Override
    public final Multiplier getMultiplier(int id) {
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_MULTIPLIER, id).executeQuery()) {
            if (res.next()) {
                return MultiplierBuilder.newBuilder(res.getString("server"), MultiplierType.valueOf(res.getString("type")), new MultiplierData(UUID.fromString(res.getString("uuid")), plugin.getName(UUID.fromString(res.getString("uuid")), false), res.getInt("amount"), res.getInt("minutes"))).setID(res.getInt("id")).setEnabled(res.getBoolean("enabled")).setQueue(res.getBoolean("queue")).build(false);
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting the multiplier with the id #" + id + " from the database.");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public final Set<Multiplier> getMultipliers(UUID uuid) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_PLAYER, uuid).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers for " + uuid);
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public final Set<Multiplier> getMultipliers(UUID uuid, String server) {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS_SERVER, uuid, server).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers for " + uuid + " in server " + server);
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        Set<Multiplier> multipliers = new LinkedHashSet<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_MULTIPLIERS).executeQuery();) {
            while (res.next()) {
                multipliers.add(getMultiplier(res.getInt("id")));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the multipliers");
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public final void enableMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.ENABLE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            plugin.log("An error has occurred enabling the multiplier #" + multiplier.getId());
            plugin.debug(ex);
        }
    }

    @Override
    public final void deleteMultiplier(Multiplier multiplier) {
        try (Connection c = getConnection()) {
            DatabaseUtils.prepareStatement(c, SQLQuery.DELETE_MULTIPLIER, multiplier.getId()).executeUpdate();
        } catch (SQLException ex) {
            plugin.log("An error has occurred while deleting the multiplier #" + multiplier.getId());
            plugin.debug(ex);
        }
    }

    @Override
    public final LinkedHashMap<String, Double> getAllPlayers() {
        LinkedHashMap<String, Double> data = new LinkedHashMap<>();
        try (Connection c = getConnection(); ResultSet res = DatabaseUtils.prepareStatement(c, SQLQuery.SELECT_ALL_PLAYERS).executeQuery()) {
            while (res.next()) {
                data.put(res.getString("nick") + "," + res.getString("uuid"), res.getDouble("balance"));
            }
        } catch (SQLException ex) {
            plugin.log("An error has occurred getting all the players from the database, check the logs for more info.");
            plugin.debug(ex);
        }
        return data;
    }

    @Override
    public final void shutdown() {
        ds.close();
    }
}
