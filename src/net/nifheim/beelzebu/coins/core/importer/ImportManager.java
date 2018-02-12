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
package net.nifheim.beelzebu.coins.core.importer;

import java.io.File;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.database.CoinsDatabase;
import net.nifheim.beelzebu.coins.core.database.MySQL;
import net.nifheim.beelzebu.coins.core.database.Redis;
import net.nifheim.beelzebu.coins.core.database.SQLite;
import net.nifheim.beelzebu.coins.core.database.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Beelzebu
 */
public class ImportManager {

    private final Core core;

    public ImportManager(Core core) {
        this.core = core;
    }

    public void importFrom(PluginToImport plugin) {
        switch (plugin) {
            case PLAYER_POINTS:
                if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
                    core.log("Seems that PlayerPoints is not installed in this server, you need to have this plugin installed to start the migration, you can remove it when it is finished.");
                    return;
                }
                core.log("Starting the migration of playerpoints data to coins, this may take a moment.");
                FileConfiguration ppConfig = Bukkit.getPluginManager().getPlugin("PlayerPoints").getConfig();
                String storageType = ppConfig.getString("storage");
                if (storageType.equalsIgnoreCase("YAML")) {
                    ConfigurationSection storage = YamlConfiguration.loadConfiguration(new File(Bukkit.getPluginManager().getPlugin("PlayerPoints").getDataFolder(), "storage.yml")).getConfigurationSection("Points");
                    storage.getKeys(false).forEach(uuid -> {
                        try {
                            UUID uuid2 = UUID.fromString(uuid);
                            double balance = storage.getDouble(uuid, 0);
                            if (CoinsAPI.isindb(uuid2)) {
                                CoinsAPI.setCoins(uuid2, balance);
                            } else {
                                CoinsAPI.createPlayer("unknow_player_from_pp", uuid2, balance);
                            }
                            core.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            core.log("An error has ocurred while migrating the data for: " + uuid);
                            core.debug(ex);
                        }
                    });
                } else if (storageType.equalsIgnoreCase("SQLITE")) {
                    try {
                        org.black_ixx.playerpoints.storage.models.SQLiteStorage sqliteStorage = new org.black_ixx.playerpoints.storage.models.SQLiteStorage((org.black_ixx.playerpoints.PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints"));
                        Field f = sqliteStorage.getClass().getDeclaredField("sqlite");
                        f.setAccessible(true);
                        lib.PatPeter.SQLibrary.SQLite sqlite = (lib.PatPeter.SQLibrary.SQLite) f.get(sqliteStorage);
                        try (PreparedStatement ps = sqlite.prepare("SELECT * FROM playerpoints;"); ResultSet res = ps.executeQuery()) {
                            while (res.next()) {
                                try {
                                    UUID uuid = UUID.fromString(res.getString("playername"));
                                    double balance = res.getInt("points");
                                    if (CoinsAPI.isindb(uuid)) {
                                        CoinsAPI.setCoins(uuid, balance);
                                    } else {
                                        CoinsAPI.createPlayer("unknow_player_from_pp", uuid, balance);
                                    }
                                    core.debug("Migrated the data for: " + uuid);
                                } catch (SQLException ex) {
                                    core.log("An error has ocurred while migrating the data for: " + res.getString("playername"));
                                    core.debug(ex);
                                }
                            }
                        } catch (SQLException ex) {
                            core.log("An error has ocurred while migrating the data from PlayerPoints");
                            core.debug(ex);
                        }
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(ImportManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if (storageType.equalsIgnoreCase("MYSQL")) {
                    try {
                        org.black_ixx.playerpoints.storage.models.MySQLStorage mysqlStorage = new org.black_ixx.playerpoints.storage.models.MySQLStorage((org.black_ixx.playerpoints.PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints"));
                        Field f = mysqlStorage.getClass().getDeclaredField("mysql");
                        f.setAccessible(true);
                        lib.PatPeter.SQLibrary.MySQL mysql = (lib.PatPeter.SQLibrary.MySQL) f.get(mysqlStorage);
                        try (PreparedStatement ps = mysql.prepare("SELECT * FROM " + ppConfig.getString("mysql.table")); ResultSet res = ps.executeQuery()) {
                            while (res.next()) {
                                try {
                                    UUID uuid = UUID.fromString(res.getString("playername"));
                                    double balance = res.getInt("points");
                                    if (CoinsAPI.isindb(uuid)) {
                                        CoinsAPI.setCoins(uuid, balance);
                                    } else {
                                        CoinsAPI.createPlayer("unknow_player_from_pp", uuid, balance);
                                    }
                                    core.debug("Migrated the data for: " + uuid);
                                } catch (SQLException ex) {
                                    core.log("An error has ocurred while migrating the data for: " + res.getString("playername"));
                                    core.debug(ex);
                                }
                            }
                        } catch (SQLException ex) {
                            core.log("An error has ocurred while migrating the data from PlayerPoints");
                            core.debug(ex);
                        }
                    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                        Logger.getLogger(ImportManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                core.log("The migration was completed, check the plugin logs for more information.");
                break;
            default:
                break;
        }
    }

    public void importFromStorage(StorageType storage) {
        switch (storage) {
            case MYSQL:
                if (core.getStorageType().equals(StorageType.MYSQL)) {
                    core.log("You can't migrate information from the same database that you are using.");
                    return;
                }
                CoinsDatabase mysql = new MySQL();
                mysql.setup();
                Map<String, Double> mysqlData = mysql.getAllPlayers();
                if (!mysqlData.isEmpty()) {
                    core.log("Starting the migration from MySQL, this may take a moment.");
                    mysqlData.entrySet().forEach(entry -> {
                        String nick = null;
                        UUID uuid = null;
                        try {
                            nick = entry.getKey().split(",")[0];
                            uuid = UUID.fromString(entry.getKey().split(",")[1]);
                            double balance = entry.getValue();
                            CoinsAPI.createPlayer(nick, uuid, balance);
                            core.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            core.log("An error has ocurred while migrating the data for: " + nick + " (" + uuid + ")");
                            core.debug(ex);
                        }
                    });
                    core.log("The migration was completed, check the plugin logs for more information.");
                } else {
                    core.log("There are no users to migrate in the database.");
                }
                mysql.shutdown();
                break;
            case SQLITE:
                if (core.getStorageType().equals(StorageType.SQLITE)) {
                    core.log("You can't migrate information from the same database that you are using.");
                    return;
                }
                CoinsDatabase sqlite = new SQLite();
                sqlite.setup();
                Map<String, Double> sqliteData = sqlite.getAllPlayers();
                if (!sqliteData.isEmpty()) {
                    core.log("Starting the migration from SQLite, this may take a moment.");
                    sqliteData.entrySet().forEach(entry -> {
                        String nick = null;
                        UUID uuid = null;
                        try {
                            nick = entry.getKey().split(",")[0];
                            uuid = UUID.fromString(entry.getKey().split(",")[1]);
                            double balance = entry.getValue();
                            CoinsAPI.createPlayer(nick, uuid, balance);
                            core.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            core.log("An error has ocurred while migrating the data for: " + nick + " (" + uuid + ")");
                            core.debug(ex);
                        }
                    });
                    core.log("The migration was completed, check the plugin logs for more information.");
                } else {
                    core.log("There are no users to migrate in the database.");
                }
                sqlite.shutdown();
                break;
            case REDIS:
                if (core.getStorageType().equals(StorageType.REDIS)) {
                    core.log("You can't migrate information from the same database that you are using.");
                    return;
                }
                CoinsDatabase redis = new Redis();
                redis.setup();
                Map<String, Double> redisData = redis.getAllPlayers();
                if (!redisData.isEmpty()) {
                    core.log("Starting the migration from Redis, this may take a moment.");
                    redisData.entrySet().forEach(entry -> {
                        String nick = null;
                        UUID uuid = null;
                        try {
                            nick = entry.getKey().split(",")[0];
                            uuid = UUID.fromString(entry.getKey().split(",")[1]);
                            double balance = entry.getValue();
                            CoinsAPI.createPlayer(nick, uuid, balance);
                            core.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            core.log("An error has ocurred while migrating the data for: " + nick + " (" + uuid + ")");
                            core.debug(ex);
                        }
                    });
                    core.log("The migration was completed, check the plugin logs for more information.");
                } else {
                    core.log("There are no users to migrate in the database.");
                }
                redis.shutdown();
                break;
            default:
                break;
        }
    }

    public enum PluginToImport {
        PLAYER_POINTS;
    }
}
