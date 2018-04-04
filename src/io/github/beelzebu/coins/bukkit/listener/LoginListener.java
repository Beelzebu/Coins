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
package io.github.beelzebu.coins.bukkit.listener;

import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.bukkit.utils.bungee.PluginMessage;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.Core;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Beelzebu
 */
public class LoginListener implements Listener {

    private final Core core = Core.getInstance();
    private static boolean first = true;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (core.getConfig().getBoolean("General.Create Join", false)) {
            CoinsAPI.createPlayer(e.getPlayer().getName(), e.getPlayer().getUniqueId());
        }
        if (!core.getConfig().useBungee()) {
            return;
        }
        PluginMessage pmsg = new PluginMessage();
        if (first) {
            Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                pmsg.sendToBungeeCord("Multiplier", "getAllMultipliers");
                pmsg.sendToBungeeCord("Coins", "getExecutors");
            }, 30);
            first = false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (core.getConfig().useBungee()) {
            return;
        }
        CacheManager.getPlayersData().invalidate(e.getPlayer().getUniqueId());
    }
}
