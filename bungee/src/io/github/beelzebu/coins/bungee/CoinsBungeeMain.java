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

import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.CoinsResponse.CoinsResponseType;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.bungee.config.BungeeConfig;
import io.github.beelzebu.coins.bungee.config.BungeeMessages;
import io.github.beelzebu.coins.bungee.events.CoinsChangeEvent;
import io.github.beelzebu.coins.bungee.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.bungee.messaging.BungeeMessaging;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.executor.ExecutorManager;
import io.github.beelzebu.coins.common.messaging.ProxyMessaging;
import io.github.beelzebu.coins.common.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.common.plugin.CoinsPlugin;
import io.github.beelzebu.coins.common.utils.dependencies.classloader.PluginClassLoader;
import io.github.beelzebu.coins.common.utils.dependencies.classloader.ReflectionClassLoader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 *
 * @author Beelzebu
 */
public class CoinsBungeeMain extends Plugin implements CoinsBootstrap {

    protected final CoinsCore core = CoinsCore.getInstance();
    private final CoinsBungeePlugin plugin;
    private BungeeConfig config;
    private BungeeMessaging bmessaging;

    public CoinsBungeeMain() {
        plugin = new CoinsBungeePlugin(this);
    }

    @Override
    public void onLoad() {
        core.setup(this);
    }

    @Override
    public void onEnable() {
        config = new BungeeConfig();
        core.start();
    }

    @Override
    public void onDisable() {
        core.shutdown();
    }

    public void execute(String executorid, ProxiedPlayer p) {
        Executor executor = ExecutorManager.getExecutor(executorid);
        if (CoinsAPI.getCoins(p.getUniqueId()) >= executor.getCost() && CoinsAPI.takeCoins(p.getUniqueId(), executor.getCost()).getResponse().equals(CoinsResponseType.SUCCESS)) {
            ExecutorManager.getExecutor(executorid).getCommands().forEach(cmd -> ProxyServer.getInstance().getPluginManager().dispatchCommand(p, cmd));
        }
    }

    @Override
    public CoinsPlugin getPlugin() {
        return plugin;
    }

    @Override
    public CoinsConfig getPluginConfig() {
        return config;
    }

    @Override
    public MessagesConfig getMessages(String lang) {
        return new BungeeMessages(lang);
    }

    @Override
    public void runAsync(Runnable rn) {
        ProxyServer.getInstance().getScheduler().runAsync(this, rn);
    }

    @Override
    public void runAsyncTimmer(Runnable rn, long timer) {
        ProxyServer.getInstance().getScheduler().schedule(this, rn, 0, timer / 20, TimeUnit.SECONDS);
    }

    @Override
    public void runTaskLater(Runnable rn, long ticks) {
        ProxyServer.getInstance().getScheduler().schedule(this, rn, ticks / 20, TimeUnit.SECONDS);
    }

    @Override
    public void runSync(Runnable rn) {
        rn.run();
    }

    @Override
    public void executeCommand(String cmd) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand((CommandSender) getConsole(), cmd);
    }

    @Override
    public void log(Object log) {
        ((CommandSender) getConsole()).sendMessage(CoinsCore.getInstance().rep("&8[&cCoins&8] &7" + log));
    }

    @Override
    public Object getConsole() {
        return ProxyServer.getInstance().getConsole();
    }

    @Override
    public void sendMessage(Object commandsender, String msg) {
        ((CommandSender) commandsender).sendMessage(TextComponent.fromLegacyText(msg));
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }

    @Override
    public boolean isOnline(String name) {
        return ProxyServer.getInstance().getPlayer(name) != null;
    }

    @Override
    public UUID getUUID(String name) {
        return ProxyServer.getInstance().getPlayer(name) != null ? ProxyServer.getInstance().getPlayer(name).getUniqueId() : null;
    }

    @Override
    public String getName(UUID uuid) {
        return ProxyServer.getInstance().getPlayer(uuid) != null ? ProxyServer.getInstance().getPlayer(uuid).getName() : null;
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        ProxyServer.getInstance().getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins));
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        ProxyServer.getInstance().getPluginManager().callEvent(new MultiplierEnableEvent(multiplier));
    }

    @Override
    public List<String> getPermissions(UUID uuid) {
        List<String> permissions = new ArrayList<>();
        if (isOnline(uuid)) {
            permissions.addAll(ProxyServer.getInstance().getPlayer(uuid).getPermissions());
        }
        return permissions;
    }

    @Override
    public ProxyMessaging getBungeeMessaging() {
        return bmessaging == null ? bmessaging = new BungeeMessaging() : bmessaging;
    }

    @Override
    public PluginClassLoader getPluginClassLoader() {
        return new ReflectionClassLoader(this);
    }

    @Override
    public InputStream getResource(String filename) {
        return getResourceAsStream(filename);
    }
}
