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
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.messaging.MessagingService;
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

    private final CoinsCore core = CoinsCore.getInstance();
    private static boolean first = true;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (core.getConfig().getBoolean("General.Create Join", false)) {
            CoinsAPI.createPlayer(e.getPlayer().getName(), e.getPlayer().getUniqueId());
        }
        if (first && core.getMessagingService().getType().equals(MessagingService.BUNGEECORD)) {
            core.getBootstrap().runAsync(() -> {
                core.getMessagingService().getMultipliers();
                core.getMessagingService().getExecutors();
            });
            first = false;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        // If you aren't using a messaging service is better invalidate the data
        // to avoid different coins between servers
        if (!core.getMessagingService().getType().equals(MessagingService.NONE)) {
            return;
        }
        CacheManager.getPlayersData().invalidate(e.getPlayer().getUniqueId());
    }
}
