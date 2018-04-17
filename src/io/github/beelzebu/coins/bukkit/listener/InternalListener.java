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

import io.github.beelzebu.coins.bukkit.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.common.CoinsCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 *
 * @author Beelzebu
 */
public class InternalListener implements Listener {

    private final CoinsCore core = CoinsCore.getInstance();

    @EventHandler
    public void onMultiplierUse(MultiplierEnableEvent e) {
        core.debug("Started multiplier check task");
        /*
        (new BukkitRunnable() {
            @Override
            public void run() {
                if (CoinsAPI.getMultiplier(e.getData().getServer()).checkTime() <= 0) {
                    core.debug("Canceling the multiplier check task");
                    cancel();
                }
            }
        }).runTaskTimerAsynchronously(Main.getInstance(), 0, 20);
         */
    }
}
