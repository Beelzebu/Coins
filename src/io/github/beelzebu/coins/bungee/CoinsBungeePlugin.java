/**
 * This file is part of Coins
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
package io.github.beelzebu.coins.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.beelzebu.coins.bungee.listener.PluginMessageListener;
import io.github.beelzebu.coins.bungee.listener.PubSubMessageListener;
import io.github.beelzebu.coins.common.messaging.MessagingService;
import io.github.beelzebu.coins.common.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.common.plugin.CoinsPlugin;
import net.md_5.bungee.api.ProxyServer;

/**
 *
 * @author Beelzebu
 */
public class CoinsBungeePlugin extends CoinsPlugin {

    private final CoinsBungeeMain bootstrap;

    public CoinsBungeePlugin(CoinsBungeeMain bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void enable() {
        super.enable();
        if (bootstrap.core.getMessagingService().getType().equals(MessagingService.BUNGEECORD)) {
            if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
                ProxyServer.getInstance().getPluginManager().registerListener(bootstrap, new PubSubMessageListener());
                RedisBungee.getApi().registerPubSubChannels("Executors", "Update");
                bootstrap.log("Using RedisBungee for plugin messaging.");
            }
            ProxyServer.getInstance().getPluginManager().registerListener(bootstrap, new PluginMessageListener());
            ProxyServer.getInstance().registerChannel("Coins");
        }
    }

    @Override
    public void disable() {
        super.disable();
        ProxyServer.getInstance().getScheduler().cancel(bootstrap);
    }

    @Override
    public CoinsBootstrap getBootstrap() {
        return bootstrap;
    }
}
