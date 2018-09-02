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
package io.github.beelzebu.coins.common.importer;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageType;
import io.github.beelzebu.coins.api.storage.sql.SQLDatabase;
import io.github.beelzebu.coins.common.storage.MySQL;
import io.github.beelzebu.coins.common.storage.SQLite;
import java.util.Map;
import java.util.UUID;
import lombok.Setter;

/**
 * @author Beelzebu
 */
public class ImportManager {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();
    @Setter
    private Importer importer = null;

    public void importFrom(PluginToImport plugin) {
        switch (plugin) {
            case PLAYER_POINTS:
                if (importer != null) {
                    importer.importFromPlayerPoints();
                } else {
                    this.plugin.log("Seems that the importer is not deffined yet.");
                }
                break;
            default:
                break;
        }
    }

    public void importFromStorage(StorageType storage) {
        switch (storage) {
            case MYSQL:
                if (plugin.getStorageType().equals(StorageType.MYSQL)) {
                    plugin.log("You can't migrate information from the same storageProvider that you are using.");
                    return;
                }
                SQLDatabase mysql = new MySQL(plugin);
                mysql.setup();
                Map<String, Double> mysqlData = mysql.getAllPlayers();
                if (!mysqlData.isEmpty()) {
                    plugin.log("Starting the migration from MySQL, this may take a moment.");
                    mysqlData.entrySet().forEach(entry -> {
                        String nick = null;
                        UUID uuid = null;
                        try {
                            nick = entry.getKey().split(",")[0];
                            uuid = UUID.fromString(entry.getKey().split(",")[1]);
                            double balance = entry.getValue();
                            CoinsAPI.createPlayer(nick, uuid, balance);
                            plugin.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            plugin.log("An error has occurred while migrating the data for: " + nick + " (" + uuid + ")");
                            plugin.debug(ex);
                        }
                    });
                    plugin.log("The migration was completed, check the plugin logs for more information.");
                } else {
                    plugin.log("There are no users to migrate in the storageProvider.");
                }
                mysql.shutdown();
                break;
            case SQLITE:
                if (plugin.getStorageType().equals(StorageType.SQLITE)) {
                    plugin.log("You can't migrate information from the same storageProvider that you are using.");
                    return;
                }
                SQLDatabase sqlite = new SQLite(plugin);
                sqlite.setup();
                Map<String, Double> sqliteData = sqlite.getAllPlayers();
                if (!sqliteData.isEmpty()) {
                    plugin.log("Starting the migration from SQLite, this may take a moment.");
                    sqliteData.entrySet().forEach(entry -> {
                        String nick = null;
                        UUID uuid = null;
                        try {
                            nick = entry.getKey().split(",")[0];
                            uuid = UUID.fromString(entry.getKey().split(",")[1]);
                            double balance = entry.getValue();
                            CoinsAPI.createPlayer(nick, uuid, balance);
                            plugin.debug("Migrated the data for: " + uuid);
                        } catch (Exception ex) {
                            plugin.log("An error has occurred while migrating the data for: " + nick + " (" + uuid + ")");
                            plugin.debug(ex);
                        }
                    });
                    plugin.log("The migration was completed, check the plugin logs for more information.");
                } else {
                    plugin.log("There are no users to migrate in the storageProvider.");
                }
                sqlite.shutdown();
                break;
            default:
                break;
        }
    }

    public enum PluginToImport {
        PLAYER_POINTS
    }
}
