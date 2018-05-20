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
package io.github.beelzebu.coins.common.importer;

import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.CoinsDatabase;
import io.github.beelzebu.coins.common.database.MySQL;
import io.github.beelzebu.coins.common.database.SQLite;
import io.github.beelzebu.coins.common.database.StorageType;
import java.util.Map;
import java.util.UUID;
import lombok.Setter;

/**
 *
 * @author Beelzebu
 */
public class ImportManager {

    private final CoinsCore core = CoinsCore.getInstance();
    @Setter
    private Importer importer = null;

    public void importFrom(PluginToImport plugin) {
        switch (plugin) {
            case PLAYER_POINTS:
                if (importer != null) {
                    importer.importFromPlayerPoints();
                } else {
                    core.log("Seems that the importer is not deffined yet.");
                }
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
            default:
                break;
        }
    }

    public enum PluginToImport {
        PLAYER_POINTS;
    }
}
