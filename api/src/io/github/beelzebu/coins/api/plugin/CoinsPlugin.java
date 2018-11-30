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
package io.github.beelzebu.coins.api.plugin;

import com.google.gson.Gson;
import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import io.github.beelzebu.coins.api.storage.StorageProvider;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public interface CoinsPlugin {

    void load();

    void enable();

    void disable();

    CoinsBootstrap getBootstrap();

    CacheProvider getCache();

    void setCache(CacheProvider cacheProvider);

    StorageProvider getStorageProvider();

    void setStorageProvider(StorageProvider storageProvider);

    StorageType getStorageType();

    void setStorageType(StorageType storageType);

    AbstractMessagingService getMessagingService();

    void setMessagingService(AbstractMessagingService messagingService);

    Gson getGson();

    void loadExecutors();

    File getMultipliersFile();

    void log(String message);

    void debug(String message);

    void debug(Exception ex);

    void debug(SQLException ex);

    String getStackTrace(Exception e);

    UUID getUniqueId(String name, boolean fromdb);

    String getName(UUID uniqueId, boolean fromdb);

    CoinsConfig getConfig();

    AbstractConfigFile getMessages(String lang);

    String getString(String path, String locale);

    List<String> getStringList(String path, String locale);

    void reloadMessages();
}
