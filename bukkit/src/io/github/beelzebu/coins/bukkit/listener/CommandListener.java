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
import io.github.beelzebu.coins.common.CoinsCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 *
 * @author Beelzebu
 */
public class CommandListener implements Listener {

    private final CoinsCore core = CoinsCore.getInstance();

    @EventHandler
    public void onCommandEvent(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage().toLowerCase();
        CoinsCore.getInstance().getBootstrap().runAsync(() -> {
            if (msg.replaceFirst("/", "").startsWith(core.getConfig().getCommand()) || core.getConfig().getCommandAliases().contains(msg.split(" ")[0].replaceFirst("/", ""))) {
                core.debug(e.getPlayer().getName() + " issued command: " + msg);
            }
            if (core.getConfig().getDouble("Command Cost." + msg) != 0) {
                if (CoinsAPI.getCoins(e.getPlayer().getUniqueId()) < core.getConfig().getDouble("Command Cost." + msg)) {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(CoinsCore.getInstance().rep(CoinsCore.getInstance().getMessages(e.getPlayer().spigot().getLocale()).getString("Errors.No Coins")));
                } else {
                    core.debug("Applied command cost for " + e.getPlayer().getName() + " in command: " + msg);
                    CoinsAPI.takeCoins(e.getPlayer().getName(), core.getConfig().getDouble("Command Cost." + msg));
                }
            }
        });
    }
}
