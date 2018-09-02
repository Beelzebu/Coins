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
package io.github.beelzebu.coins.api.config;

import io.github.beelzebu.coins.api.messaging.MessagingService;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author Beelzebu
 */
public abstract class CoinsConfig extends AbstractConfigFile {

    protected final CoinsPlugin plugin;

    public CoinsConfig(File file, CoinsPlugin plugin) {
        super(file);
        this.plugin = plugin;
    }

    // #EasterEgg
    public boolean vaultMultipliers() {
        return getBoolean("Vault.Use Multipliers", false);
    }

    public boolean useBungee() {
        return plugin.getMessagingService().getType().equals(MessagingService.BUNGEECORD);
    }

    public boolean isOnline() {
        return getBoolean("Online Mode", true);
    }

    public List<String> getCommandAliases() {
        return getStringList("General.Command.Aliases", Collections.emptyList());
    }

    public String getCommand() {
        return getString("General.Command.Name", "coins");
    }

    public String getServerName() {
        return getString("Multipliers.Server", "default");
    }

    public StorageType getStorageType() {
        StorageType type = StorageType.SQLITE;
        try {
            return StorageType.valueOf(getString("Storage Type", "sqlite").toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.log("You have defined a invalid storage type in the config.");
        }
        return type;
    }

    public MessagingService getMessagingService() {
        MessagingService type = MessagingService.NONE;
        try {
            return MessagingService.valueOf(getString("Messaging Service", "none").toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.log("You have defined a invalid storage type in the config.");
        }
        return type;
    }

    public boolean isDebug() {
        return getBoolean("General.Logging.Debug.Enabled", false);
    }

    public boolean isDebugFile() {
        return getBoolean("General.Logging.Debug.File", true);
    }
}
