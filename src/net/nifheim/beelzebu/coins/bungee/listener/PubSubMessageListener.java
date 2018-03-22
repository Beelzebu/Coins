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
package net.nifheim.beelzebu.coins.bungee.listener;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import java.util.Collections;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.nifheim.beelzebu.coins.core.CacheManager;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;

/**
 *
 * @author Beelzebu
 */
public class PubSubMessageListener extends CoinsBungeeListener implements Listener {

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent e) {
        switch (e.getChannel()) {
            case "Coins":
                if (e.getMessage().equals("getExecutors")) {
                    core.debug("Sending executors");
                    ProxyServer.getInstance().getServers().values().forEach((server) -> {
                        sendExecutors(server);
                        core.debug("Sending to " + server.getName());
                    });
                }
                break;
            case "Update":
                ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                    sendToBukkit("Update", Collections.singletonList(e.getMessage()), ProxyServer.getInstance().getServerInfo(server), true);
                });
                break;
            case "Multiplier":
                if (e.getMessage().startsWith("disable ")) {
                    CacheManager.getMultiplier(e.getMessage().split(" ")[1]).disable();
                } else {
                    Multiplier multiplier = Multiplier.fromJson(e.getMessage(), true);
                    CacheManager.addMultiplier(multiplier.getServer(), multiplier);
                    ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                        sendToBukkit("Multiplier", Collections.singletonList(multiplier.toJson().toString()), ProxyServer.getInstance().getServerInfo(server), false);
                    });
                }
                break;
            default:
                break;
        }
    }
}
