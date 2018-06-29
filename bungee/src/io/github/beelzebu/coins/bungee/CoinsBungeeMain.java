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

import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.messaging.ProxyMessaging;
import io.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bungee.config.BungeeConfig;
import io.github.beelzebu.coins.bungee.config.BungeeMessages;
import io.github.beelzebu.coins.bungee.events.CoinsChangeEvent;
import io.github.beelzebu.coins.bungee.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.bungee.messaging.BungeeMessaging;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * @author Beelzebu
 */
public class CoinsBungeeMain extends Plugin implements CoinsBootstrap {

    private CoinsBungeePlugin plugin;
    private BungeeConfig config;
    private BungeeMessaging messaging;

    @Override
    public void onLoad() {
        plugin = new CoinsBungeePlugin(this);
        plugin.load();
    }

    @Override
    public void onEnable() {
        config = new BungeeConfig(new File(getDataFolder(), "config.yml"), plugin);
        plugin.enable();
    }

    @Override
    public void onDisable() {
        plugin.disable();
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
    public AbstractConfigFile getFileAsConfig(File file) {
        return new BungeeMessages(file);
    }

    @Override
    public void runAsync(Runnable rn) {
        ProxyServer.getInstance().getScheduler().runAsync(this, rn);
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
    public void log(String msg) {
        ((CommandSender) getConsole()).sendMessage(TextComponent.fromLegacyText(StringUtils.rep("&8[&cCoins&8] &7" + msg)));
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
        return messaging == null ? messaging = new BungeeMessaging() : messaging;
    }

    @Override
    public InputStream getResource(String filename) {
        return getResourceAsStream(filename);
    }
}
