/**
 * This file is part of Coins
 *
 * Copyright (C) 2018 Beelzebu
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
package io.github.beelzebu.coins.bukkit.importer;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.common.importer.ImportManager;
import io.github.beelzebu.coins.common.importer.Importer;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Beelzebu
 */
public class BukkitImporter implements Importer {

    @Override
    public void importFromPlayerPoints() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            plugin.log("Seems that PlayerPoints is not installed in this server, you need to have this plugin installed to start the migration, you can remove it when it is finished.");
            return;
        }
        plugin.log("Starting the migration of playerpoints data to coins, this may take a moment.");
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
                    plugin.debug("Migrated the data for: " + uuid);
                } catch (Exception ex) {
                    plugin.log("An error has ocurred while migrating the data for: " + uuid);
                    plugin.debug(ex);
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
                            plugin.debug("Migrated the data for: " + uuid);
                        } catch (SQLException ex) {
                            plugin.log("An error has ocurred while migrating the data for: " + res.getString("playername"));
                            plugin.debug(ex);
                        }
                    }
                } catch (SQLException ex) {
                    plugin.log("An error has ocurred while migrating the data from PlayerPoints");
                    plugin.debug(ex);
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
                            plugin.debug("Migrated the data for: " + uuid);
                        } catch (SQLException ex) {
                            plugin.log("An error has ocurred while migrating the data for: " + res.getString("playername"));
                            plugin.debug(ex);
                        }
                    }
                } catch (SQLException ex) {
                    plugin.log("An error has ocurred while migrating the data from PlayerPoints");
                    plugin.debug(ex);
                }
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(ImportManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        plugin.log("The migration was completed, check the plugin logs for more information.");
    }

}
