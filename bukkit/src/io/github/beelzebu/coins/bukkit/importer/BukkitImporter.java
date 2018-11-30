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
package io.github.beelzebu.coins.bukkit.importer;

import com.twanl.tokens.commands.Commands;
import com.twanl.tokens.lib.Lib;
import com.twanl.tokens.sql.SQLlib;
import com.twanl.tokens.utils.ConfigManager;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.common.importer.Importer;
import io.github.beelzebu.coins.common.importer.PluginToImport;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import lib.PatPeter.SQLibrary.MySQL;
import lib.PatPeter.SQLibrary.SQLite;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.storage.models.MySQLStorage;
import org.black_ixx.playerpoints.storage.models.SQLiteStorage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Beelzebu
 */
public class BukkitImporter implements Importer {

    @Override
    public void importFrom(PluginToImport pluginToImport) {
        switch (pluginToImport) {
            case PLAYER_POINTS:
                importFromPlayerPoints();
                break;
            case DKCOINS:
                importFromDKCoins();
                break;
            case TOKENS_ECONOMY:
                importFromTokensEconomy();
                break;
            default:
                throw new RuntimeException("Not implemented yet.");
        }
    }

    private void importFromPlayerPoints() {
        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
            Importer.plugin.log("Seems that PlayerPoints is not installed in this server, you need to have this plugin installed to start the migration, you can remove it when it is finished.");
            return;
        }
        Importer.plugin.log("Starting the migration of playerpoints data to coins, this may take a moment.");
        FileConfiguration ppConfig = JavaPlugin.getPlugin(PlayerPoints.class).getConfig();
        String storageType = ppConfig.getString("storage");
        switch (storageType.toUpperCase()) {
            case "YAML":
                ConfigurationSection storage = YamlConfiguration.loadConfiguration(new File(JavaPlugin.getPlugin(PlayerPoints.class).getDataFolder(), "storage.yml")).getConfigurationSection("Points");
                storage.getKeys(false).stream().map(key -> {
                    try {
                        return UUID.fromString(key);
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(Objects::nonNull).forEach(uuid -> migrate(uuid, "unknown_from_pp", storage.getDouble(uuid.toString(), 0)));
                break;
            case "SQLITE":
                try {
                    SQLiteStorage sqliteStorage = new SQLiteStorage((PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints"));
                    Field f = sqliteStorage.getClass().getDeclaredField("sqlite");
                    f.setAccessible(true);
                    SQLite sqlite = (SQLite) f.get(sqliteStorage);
                    try (PreparedStatement ps = sqlite.prepare("SELECT * FROM playerpoints;"); ResultSet res = ps.executeQuery()) {
                        while (res.next()) {
                            try {
                                UUID uuid = UUID.fromString(res.getString("playername"));
                                double balance = res.getInt("points");
                                migrate(uuid, "unknown_from_pp", balance);
                            } catch (SQLException ex) {
                                Importer.plugin.log("An error has occurred while migrating the data for: " + res.getString("playername"));
                                Importer.plugin.debug(ex);
                            }
                        }
                    } catch (SQLException ex) {
                        Importer.plugin.log("An error has occurred while migrating the data from PlayerPoints");
                        Importer.plugin.debug(ex);
                    }
                } catch (ReflectiveOperationException ex) {
                    Importer.plugin.log(Importer.plugin.getStackTrace(ex));
                }
                break;
            case "MYSQL":
                try {
                    MySQLStorage mysqlStorage = new MySQLStorage((PlayerPoints) Bukkit.getPluginManager().getPlugin("PlayerPoints"));
                    Field f = mysqlStorage.getClass().getDeclaredField("mysql");
                    f.setAccessible(true);
                    MySQL mysql = (MySQL) f.get(mysqlStorage);
                    try (PreparedStatement ps = mysql.prepare("SELECT * FROM " + ppConfig.getString("mysql.table")); ResultSet res = ps.executeQuery()) {
                        while (res.next()) {
                            try {
                                UUID uuid = UUID.fromString(res.getString("playername"));
                                double balance = res.getInt("points");
                                migrate(uuid, "unknown_from_pp", balance);
                            } catch (SQLException ex) {
                                Importer.plugin.log("An error has occurred while migrating the data for: " + res.getString("playername"));
                                Importer.plugin.debug(ex);
                            }
                        }
                    } catch (SQLException ex) {
                        Importer.plugin.log("An error has occurred while migrating the data from PlayerPoints");
                        Importer.plugin.debug(ex);
                    }
                } catch (ReflectiveOperationException ex) {
                    Importer.plugin.log(Importer.plugin.getStackTrace(ex));
                }
                break;
        }
        Importer.plugin.log("The migration was completed, check the plugin logs for more information.");
    }

    private void importFromDKCoins() {
        if (Bukkit.getPluginManager().getPlugin("DKCoins") == null) {
            Importer.plugin.log("Seems that DKCoins is not installed in this server, you need to have this plugin installed to start the migration, you can remove it when it is finished.");
            return;
        }
        Importer.plugin.log("Starting the migration of DKCoins data to coins, this may take a moment.");
        ch.dkrieger.coinsystem.core.CoinSystem.getInstance().getPlayerManager().getPlayers().stream().filter(coinPlayer -> coinPlayer != null && coinPlayer.getUUID() != null && coinPlayer.getName() != null).forEach(coinPlayer -> migrate(coinPlayer.getUUID(), coinPlayer.getName(), coinPlayer.getCoins()));
        Importer.plugin.log("The migration was completed, check the plugin logs for more information.");
    }

    private void importFromTokensEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Tokens") == null) {
            Importer.plugin.log("Seems that Tokens is not installed in this server, you need to have this plugin installed to start the migration, you can remove it when it is finished.");
            return;
        }
        Importer.plugin.log("Starting the migration of Tokens data to coins, this may take a moment.");
        ConfigManager configManager = new ConfigManager();
        SQLlib sqLlib = new SQLlib();
        Lib lib = new Lib();
        if (lib.sqlUse()) {
            try {
                sqLlib.getAllRowstoHashMap();
                Commands.map.forEach((uuid, integer) -> migrate(uuid, "unknown_from_te", integer));
            } catch (Exception ex) {
                Importer.plugin.debug(ex);
            }
        } else {
            configManager.getPlayers().getKeys(false).stream().map(key -> {
                try {
                    return UUID.fromString(key);
                } catch (Exception e) {
                    return null;
                }
            }).filter(Objects::nonNull).forEach(uuid -> migrate(uuid, "unknown_from_te", configManager.getPlayers().getInt(uuid + ".tokens")));
        }
        Importer.plugin.log("The migration was completed, check the plugin logs for more information.");
    }

    private void migrate(UUID uuid, String name, Number balance) {
        try {
            if (CoinsAPI.isindb(uuid)) {
                CoinsAPI.addCoins(uuid, balance.doubleValue(), false);
            } else if (CoinsAPI.isindb(name)) {
                CoinsAPI.addCoins(name, balance.doubleValue(), false);
            } else {
                CoinsAPI.createPlayer(name, uuid, balance.doubleValue());
            }
            Importer.plugin.debug("Migrated the data for: '" + uuid + "' (" + name + ")");
        } catch (Exception ex) {
            Importer.plugin.log("There is an error migrating data for: '" + uuid + "' (" + name + "), check logs for more information.");
            Importer.plugin.debug(ex);
        }
    }
}
