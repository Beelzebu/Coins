/*
 * This file is part of Coins.
 *
 * Copyright © 2017 Beelzebu
 * Coins is licensed under the GNU General Public License.
 *
 * Coins is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coins is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.coins.core.utils;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.database.StorageType;
import net.nifheim.beelzebu.coins.core.interfaces.IConfiguration;
import org.bukkit.Bukkit;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsConfig implements IConfiguration {

    @Getter
    @Setter
    private MessagingService messagingService;

    // #EasterEgg
    public boolean vaultMultipliers() {
        return getBoolean("Vault.Use Multipliers", false);
    }

    public boolean useBungee() {
        try {
            return Bukkit.spigot().getConfig().getBoolean("settings.bungeecord") && !Core.getInstance().getStorageType().equals(StorageType.REDIS);
        } catch (Exception ex) {
            return false;
        }
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
}
