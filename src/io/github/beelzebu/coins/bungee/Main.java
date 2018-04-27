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
package io.github.beelzebu.coins.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.CoinsResponse.CoinsResponseType;
import io.github.beelzebu.coins.bungee.listener.PluginMessageListener;
import io.github.beelzebu.coins.bungee.listener.PubSubMessageListener;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.messaging.MessagingServiceType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 *
 * @author Beelzebu
 */
public class Main extends Plugin {

    private static Main instance;
    private final CoinsCore core = CoinsCore.getInstance();
    private static boolean useRedis = false;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        core.setup(new BungeeMethods());
    }

    @Override
    public void onEnable() {
        core.start();
        if (core.getMessagingService().getType().equals(MessagingServiceType.BUNGEECORD)) {
            if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
                ProxyServer.getInstance().getPluginManager().registerListener(this, new PubSubMessageListener());
                RedisBungee.getApi().registerPubSubChannels("Executors", "Update");
                useRedis = true;
                core.log("Using RedisBungee for plugin messaging.");
            }
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PluginMessageListener());
            ProxyServer.getInstance().registerChannel("Coins");
        }
        loadExecutors();
    }

    @Override
    public void onDisable() {
        ProxyServer.getInstance().getScheduler().cancel(this);
        core.shutdown();
    }

    public void execute(String executorid, ProxiedPlayer p) {
        Executor executor = core.getExecutorManager().getExecutor(executorid);
        if (CoinsAPI.getCoins(p.getUniqueId()) >= executor.getCost() && CoinsAPI.takeCoins(p.getUniqueId(), executor.getCost()).getResponse().equals(CoinsResponseType.SUCCESS)) {
            core.getExecutorManager().getExecutor(executorid).getCommands().forEach(cmd -> ProxyServer.getInstance().getPluginManager().dispatchCommand(p, cmd));
        }
    }

    public boolean useRedisBungee() {
        return useRedis;
    }

    private void loadExecutors() {
        core.getConfig().getConfigurationSection("Command executor").forEach(id -> core.getExecutorManager().addExecutor(new Executor(id, core.getConfig().getString("Command executor." + id + ".Displayname", id), core.getConfig().getDouble("Command executor." + id + ".Cost", 0), core.getConfig().getStringList("Command executor." + id + ".Command"))));
    }
}
