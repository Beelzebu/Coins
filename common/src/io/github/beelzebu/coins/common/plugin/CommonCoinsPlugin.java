/**
 * This file is part of coins-common
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
package io.github.beelzebu.coins.common.plugin;

import com.google.common.collect.Lists;
import com.google.gson.JsonParseException;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.messaging.MessagingService;
import io.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageProvider;
import io.github.beelzebu.coins.api.storage.StorageType;
import io.github.beelzebu.coins.api.storage.sql.SQLDatabase;
import io.github.beelzebu.coins.common.database.MySQL;
import io.github.beelzebu.coins.common.database.SQLite;
import io.github.beelzebu.coins.common.messaging.DummyMessaging;
import io.github.beelzebu.coins.common.messaging.RedisMessaging;
import io.github.beelzebu.coins.common.utils.FileManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

/**
 *
 * @author Beelzebu
 */
public abstract class CommonCoinsPlugin extends CoinsPlugin {

    private SQLDatabase db;

    public CommonCoinsPlugin(CoinsBootstrap bootstrap) {
        super(bootstrap);
    }

    @Override
    public void load() {
        super.load();
        new FileManager().copyFiles();
    }

    @Override
    public void enable() { // now the plugin is enabled and we can read config files
        logEnabled = getConfig().isDebugFile();
        // identify storage type and start messaging service before start things
        storageType = getConfig().getStorageType();
        getDependencyManager().loadStorageDependencies(storageType);
        if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.BUNGEECORD.toString())) {
            messagingService = getBootstrap().getBungeeMessaging();
        } else if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.REDIS.toString())) {
            messagingService = new RedisMessaging();
        } else {
            messagingService = new DummyMessaging();
        }
        try {
            if (!getMultipliersFile().exists()) {
                getMultipliersFile().createNewFile();
            }
            Iterator<String> lines = Files.readAllLines(getMultipliersFile().toPath()).iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                try {
                    Multiplier multiplier = Multiplier.fromJson(line, false);
                    getCache().addMultiplier(multiplier);
                } catch (JsonParseException ignore) { // Invalid line
                    debug(line + " isn't a valid multiplier in json format.");
                    lines.remove();
                }
            }
            Files.write(getMultipliersFile().toPath(), Lists.newArrayList(lines));
        } catch (IOException ex) {
            log("An error has ocurred loading multipliers from local storage.");
            debug(ex.getMessage());
        }
        if (storageType.equals(StorageType.SQLITE) && getConfig().getInt("Database Version", 1) < 2) {
            try {
                Files.move(new File(getBootstrap().getDataFolder(), "database.db").toPath(), new File(getBootstrap().getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has ocurred moving the old database");
                debug(ex.getMessage());
            }
        }
        getDatabase().setup();
        messagingService.start();
        motd(true);
        getMessagingService().getMultipliers();
        getMessagingService().getExecutors();
        loadExecutors();
    }

    @Override
    public StorageProvider getDatabase() {
        if (db != null) {
            return db;
        }
        switch (storageType) {
            case MARIADB:
            case MYSQL:
                return db = new MySQL();
            case SQLITE:
                return db = new SQLite();
            default:
                return null;
        }
    }

}
