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
package io.github.beelzebu.coins.common.config;

import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.messaging.MessagingService;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsConfig extends AbstractConfigFile {

    protected final CoinsCore core = CoinsCore.getInstance();

    public CoinsConfig(File file) {
        super(file);
    }

    // #EasterEgg
    public boolean vaultMultipliers() {
        return getBoolean("Vault.Use Multipliers", false);
    }

    public boolean useBungee() {
        return core.getMessagingService().getType().equals(MessagingService.BUNGEECORD);
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
            core.log("You have deffined a invalid storage type in the config.");
        }
        return type;
    }

    public MessagingService getMessagingService() {
        MessagingService type = MessagingService.NONE;
        try {
            return MessagingService.valueOf(getString("Messaging Service", "none").toUpperCase());
        } catch (IllegalArgumentException ex) {
            core.log("You have deffined a invalid storage type in the config.");
        }
        return type;
    }
}
