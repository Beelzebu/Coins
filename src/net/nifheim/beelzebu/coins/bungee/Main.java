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
package net.nifheim.beelzebu.coins.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.CoinsResponse.CoinsResponseType;
import net.nifheim.beelzebu.coins.bungee.listener.PluginMessageListener;
import net.nifheim.beelzebu.coins.bungee.listener.PubSubMessageListener;
import net.nifheim.beelzebu.coins.bungee.utils.Configuration;
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.executor.Executor;
import net.nifheim.beelzebu.coins.core.utils.CoinsConfig;

/**
 *
 * @author Beelzebu
 */
public class Main extends Plugin {

    private static Main instance;
    private final Core core = Core.getInstance();
    private Configuration config;
    private static Boolean useRedis = false;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        config = new Configuration();
        core.setup(new BungeeMethods());
    }

    @Override
    public void onEnable() {
        core.start();
        if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PubSubMessageListener());
            RedisBungee.getApi().registerPubSubChannels("Coins", "Update", "Multiplier");
            useRedis = true;
            core.log("Using RedisBungee for plugin messaging.");
        }
        ProxyServer.getInstance().getPluginManager().registerListener(this, new PluginMessageListener());
        ProxyServer.getInstance().registerChannel("Coins");
    }

    @Override
    public void onDisable() {
        core.shutdown();
    }

    public void execute(String executorid, ProxiedPlayer p) {
        Executor executor = core.getExecutorManager().getExecutor(executorid);
        if (CoinsAPI.getCoins(p.getUniqueId()) >= executor.getCost() && CoinsAPI.takeCoins(p.getUniqueId(), executor.getCost()).getResponse().equals(CoinsResponseType.SUCCESS)) {
            core.getExecutorManager().getExecutor(executorid).getCommands().forEach(cmd -> ProxyServer.getInstance().getPluginManager().dispatchCommand(p, cmd));
        }
    }

    public CoinsConfig getConfiguration() {
        return config;
    }

    public Boolean useRedis() {
        return useRedis;
    }
}
